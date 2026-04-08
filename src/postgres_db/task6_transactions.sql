-- ================================================================
-- task6_transactions.sql
-- Task 6.7 — Transactions demonstration
-- Database: Supabase / PostgreSQL
-- ================================================================
-- Run each section separately in the Supabase SQL Editor.
-- ================================================================


-- ================================================================
-- SECTION A: Valid multi-step transaction → COMMIT
-- ================================================================
-- Scenario: investor 1 (buyer) buys 5 units of asset 2 from
--           investor 4 (seller) at $1000 per unit.
-- All 5 steps succeed → COMMIT persists everything atomically.
-- ================================================================

BEGIN;

    -- Step 1: Insert a BUY trade order for investor 1
    -- investor 1 wants to buy 5 units of asset 2 at $1000 each
    INSERT INTO trade_order (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
    VALUES (
        (SELECT COALESCE(MAX(order_id), 0) + 1 FROM trade_order),
        1, 2, 'BUY', 1000.00, 5, CURRENT_DATE, 'OPEN'
    );

    -- Step 2: Insert a matching SELL trade order for investor 4
    -- investor 4 offers 5 units of asset 2 at $1000 each
    INSERT INTO trade_order (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
    VALUES (
        (SELECT COALESCE(MAX(order_id), 0) + 1 FROM trade_order),
        4, 2, 'SELL', 1000.00, 5, CURRENT_DATE, 'OPEN'
    );

    -- Step 3: Insert the trade record that links both orders
    -- trade_price uses the seller's asking price
    INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES (
        (SELECT COALESCE(MAX(trade_id), 0) + 1 FROM trade),
        1000.00,
        5,
        CURRENT_DATE,
        (SELECT MAX(order_id) FROM trade_order WHERE investor_id = 1 AND order_type = 'BUY'  AND status = 'OPEN'),
        (SELECT MAX(order_id) FROM trade_order WHERE investor_id = 4 AND order_type = 'SELL' AND status = 'OPEN')
    );

    -- Step 4: Update ownership
    -- Buyer gains 5 units (upsert in case they already own some)
    INSERT INTO ownership (investor_id, asset_id, units_held)
    VALUES (1, 2, 5)
    ON CONFLICT (investor_id, asset_id)
    DO UPDATE SET units_held = ownership.units_held + 5;

    -- Seller loses 5 units
    UPDATE ownership
    SET    units_held = units_held - 5
    WHERE  investor_id = 4 AND asset_id = 2;

    -- Step 5: Mark both orders as MATCHED
    UPDATE trade_order
    SET    status = 'MATCHED'
    WHERE  investor_id = 1 AND order_type = 'BUY'  AND status = 'OPEN' AND asset_id = 2;

    UPDATE trade_order
    SET    status = 'MATCHED'
    WHERE  investor_id = 4 AND order_type = 'SELL' AND status = 'OPEN' AND asset_id = 2;

-- All steps succeeded — commit makes everything permanent and visible to other sessions
COMMIT;

-- Verify the results
SELECT 'trade row'  AS entity, trade_id::text AS id, trade_units::text AS detail FROM trade ORDER BY trade_id DESC LIMIT 1;
SELECT 'ownership'  AS entity, investor_id::text AS id, units_held::text AS detail FROM ownership WHERE investor_id IN (1,4) AND asset_id = 2;
SELECT 'order status' AS entity, order_id::text AS id, status AS detail FROM trade_order ORDER BY order_id DESC LIMIT 2;


-- ================================================================
-- SECTION B: Invalid transaction → ROLLBACK
-- ================================================================
-- Scenario: attempt to subtract 9999 units from a seller who
--           does not hold that many.
-- The CHECK constraint (units_held >= 0) rejects it.
-- ROLLBACK leaves the database exactly as it was.
-- ================================================================

-- Snapshot state before attempt
SELECT investor_id, asset_id, units_held AS "units_before_rollback_test"
FROM   ownership
WHERE  investor_id = 4 AND asset_id = 2;

BEGIN;

    -- This will fail — investor 4 does not hold 9999 units of asset 2.
    -- Either the CHECK constraint fires, or the application detects the
    -- negative value and explicitly rolls back.
    UPDATE ownership
    SET    units_held = units_held - 9999
    WHERE  investor_id = 4 AND asset_id = 2;

    -- If the above somehow did not error (no CHECK constraint in schema),
    -- we detect the bad state and roll back manually:
    DO $$
    DECLARE v_units INT;
    BEGIN
        SELECT units_held INTO v_units FROM ownership WHERE investor_id = 4 AND asset_id = 2;
        IF v_units < 0 THEN
            RAISE EXCEPTION 'Validation failed: units_held cannot be negative (value: %)', v_units;
        END IF;
    END;
    $$;

-- ROLLBACK — nothing is persisted; DB is unchanged
ROLLBACK;

-- Confirm state is unchanged — should show the same value as before
SELECT investor_id, asset_id, units_held AS "units_after_rollback (must match before)"
FROM   ownership
WHERE  investor_id = 4 AND asset_id = 2;
