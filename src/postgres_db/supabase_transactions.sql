-- ================================================================
-- TASK 6: TRANSACTIONS FILE (POSTGRESQL / SUPABASE VERSION)
-- Project  : Fractional Ownership Platform
-- Database : Supabase / PostgreSQL
--
-- FILE LOCATION: src/postgres_db/supabase_transactions.sql
--
-- Converted from: src/db/transactions.sql  (MySQL version)
--
-- Key MySQL → PostgreSQL changes:
--   * DELIMITER $$ / END$$        → removed; plpgsql uses AS $$ ... $$
--   * DECLARE EXIT HANDLER         → EXCEPTION WHEN OTHERS THEN
--   * proc_label: BEGIN/LEAVE      → RETURN inside plpgsql block
--   * TINYINT(1) OUT param         → BOOLEAN
--   * ON DUPLICATE KEY UPDATE      → INSERT ... ON CONFLICT DO UPDATE
--   * CURDATE()                    → CURRENT_DATE
--   * USE <db>                     → removed (Supabase single schema)
--   * Inline START TRANSACTION     → plpgsql block is always atomic;
--                                    caller wraps in BEGIN/COMMIT
-- ================================================================


-- ================================================================
-- PROCEDURE 1: sp_subscribe_to_ipo
-- Investor subscribes to an active IPO atomically.
-- FOR UPDATE locks on ipo + investor prevent over-subscription.
-- ================================================================

CREATE OR REPLACE FUNCTION sp_subscribe_to_ipo(
    p_investor_id INT,
    p_ipo_id      INT,
    p_units       INT,
    OUT p_success BOOLEAN,
    OUT p_message TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_asset_id     INT;
    v_price        NUMERIC(10,2);
    v_total        NUMERIC(15,2);
    v_balance      NUMERIC(15,2);
    v_ipo_start    DATE;
    v_ipo_end      DATE;
    v_total_units  INT;
    v_units_sold   INT;
    v_units_before INT := 0;
BEGIN
    IF p_units <= 0 THEN
        p_success := FALSE;
        p_message := 'Units must be greater than 0.';
        RETURN;
    END IF;

    -- Lock IPO row so no parallel session can change units_sold
    SELECT asset_id, price_per_unit, ipo_start_date, ipo_end_date,
           total_units, units_sold
    INTO   v_asset_id, v_price, v_ipo_start, v_ipo_end, v_total_units, v_units_sold
    FROM   ipo
    WHERE  ipo_id = p_ipo_id
    FOR UPDATE;

    IF v_asset_id IS NULL THEN
        p_success := FALSE;
        p_message := FORMAT('IPO #%s does not exist.', p_ipo_id);
        RETURN;
    END IF;

    IF CURRENT_DATE < v_ipo_start OR CURRENT_DATE > v_ipo_end THEN
        p_success := FALSE;
        p_message := FORMAT('IPO #%s is not currently active.', p_ipo_id);
        RETURN;
    END IF;

    IF v_units_sold + p_units > v_total_units THEN
        p_success := FALSE;
        p_message := FORMAT('Not enough units left. Available: %s, Requested: %s',
                             v_total_units - v_units_sold, p_units);
        RETURN;
    END IF;

    -- Lock investor wallet row
    SELECT wallet_balance INTO v_balance
    FROM   investor
    WHERE  investor_id = p_investor_id
    FOR UPDATE;

    IF v_balance IS NULL THEN
        p_success := FALSE;
        p_message := FORMAT('Investor #%s does not exist.', p_investor_id);
        RETURN;
    END IF;

    v_total := p_units * v_price;

    IF v_balance < v_total THEN
        p_success := FALSE;
        p_message := FORMAT('Insufficient balance. Required: %s, Available: %s',
                             v_total, v_balance);
        RETURN;
    END IF;

    -- Lock existing ownership row if present
    SELECT COALESCE(units_held, 0) INTO v_units_before
    FROM   ownership
    WHERE  investor_id = p_investor_id AND asset_id = v_asset_id
    FOR UPDATE;

    -- All checks passed — perform writes
    UPDATE investor SET wallet_balance = wallet_balance - v_total
    WHERE  investor_id = p_investor_id;

    UPDATE ipo SET units_sold = units_sold + p_units
    WHERE  ipo_id = p_ipo_id;

    INSERT INTO ownership (investor_id, asset_id, units_held)
    VALUES (p_investor_id, v_asset_id, p_units)
    ON CONFLICT (investor_id, asset_id)
    DO UPDATE SET units_held = ownership.units_held + EXCLUDED.units_held;

    INSERT INTO wallet_transaction
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (p_investor_id, -v_total, 'ASSET_PURCHASE', 'IPO Subscription', NOW());

    INSERT INTO ownership_history
        (investor_id, asset_id, units_before, units_after, change_date, change_type, trade_id, ipo_id)
    VALUES (p_investor_id, v_asset_id, v_units_before, v_units_before + p_units,
            CURRENT_DATE, 'IPO', NULL, p_ipo_id);

    p_success := TRUE;
    p_message := FORMAT(
        'IPO subscription successful. Investor %s bought %s unit(s) in IPO #%s. Total charged: %s',
        p_investor_id, p_units, p_ipo_id, v_total);

EXCEPTION WHEN OTHERS THEN
    p_success := FALSE;
    p_message := FORMAT('Transaction failed and rolled back: %s', SQLERRM);
    RAISE;
END;
$$;

-- Usage: BEGIN; SELECT * FROM sp_subscribe_to_ipo(2, 2, 5); COMMIT;


-- ================================================================
-- PROCEDURE 2: sp_p2p_transfer
-- Investor A transfers units directly to Investor B for a price.
-- FOR UPDATE locks both ownership rows + both wallet rows.
-- ================================================================

CREATE OR REPLACE FUNCTION sp_p2p_transfer(
    p_sender   INT,
    p_receiver INT,
    p_asset    INT,
    p_units    INT,
    p_price    NUMERIC(10,2),
    OUT p_success BOOLEAN,
    OUT p_message TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_units   INT := 0;
    v_balance NUMERIC(15,2);
    v_total   NUMERIC(15,2);
BEGIN
    IF p_units <= 0 THEN
        p_success := FALSE; p_message := 'Units must be greater than 0.'; RETURN;
    END IF;
    IF p_price < 0 THEN
        p_success := FALSE; p_message := 'Price cannot be negative.'; RETURN;
    END IF;
    IF p_sender = p_receiver THEN
        p_success := FALSE; p_message := 'Sender and receiver cannot be the same.'; RETURN;
    END IF;

    -- Lock sender ownership — prevents parallel transfer of same units
    SELECT units_held INTO v_units
    FROM   ownership
    WHERE  investor_id = p_sender AND asset_id = p_asset
    FOR UPDATE;

    IF v_units IS NULL OR v_units < p_units THEN
        p_success := FALSE;
        p_message := FORMAT('Sender does not hold enough units. Holds: %s, Requested: %s',
                             COALESCE(v_units, 0), p_units);
        RETURN;
    END IF;

    v_total := p_units * p_price;

    -- Lock receiver wallet
    SELECT wallet_balance INTO v_balance
    FROM   investor WHERE investor_id = p_receiver FOR UPDATE;

    IF v_balance IS NULL THEN
        p_success := FALSE;
        p_message := FORMAT('Receiver investor #%s does not exist.', p_receiver);
        RETURN;
    END IF;

    IF v_balance < v_total THEN
        p_success := FALSE;
        p_message := FORMAT('Receiver has insufficient funds. Required: %s, Available: %s',
                             v_total, v_balance);
        RETURN;
    END IF;

    -- Lock sender wallet too
    SELECT wallet_balance INTO v_balance
    FROM   investor WHERE investor_id = p_sender FOR UPDATE;

    -- Perform writes
    UPDATE ownership SET units_held = units_held - p_units
    WHERE  investor_id = p_sender AND asset_id = p_asset;

    DELETE FROM ownership
    WHERE  investor_id = p_sender AND asset_id = p_asset AND units_held = 0;

    INSERT INTO ownership (investor_id, asset_id, units_held)
    VALUES (p_receiver, p_asset, p_units)
    ON CONFLICT (investor_id, asset_id)
    DO UPDATE SET units_held = ownership.units_held + EXCLUDED.units_held;

    UPDATE investor SET wallet_balance = wallet_balance - v_total WHERE investor_id = p_receiver;
    UPDATE investor SET wallet_balance = wallet_balance + v_total WHERE investor_id = p_sender;

    INSERT INTO wallet_transaction
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (p_receiver, -v_total, 'ASSET_PURCHASE',
            FORMAT('P2P Transfer – Asset #%s from Investor #%s', p_asset, p_sender), NOW());

    INSERT INTO wallet_transaction
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (p_sender, v_total, 'ASSET_SALE',
            FORMAT('P2P Transfer – Asset #%s to Investor #%s', p_asset, p_receiver), NOW());

    p_success := TRUE;
    p_message := FORMAT(
        'P2P transfer successful. Investor %s transferred %s unit(s) of Asset #%s to Investor %s for %s',
        p_sender, p_units, p_asset, p_receiver, v_total);

EXCEPTION WHEN OTHERS THEN
    p_success := FALSE;
    p_message := FORMAT('P2P transfer failed and rolled back: %s', SQLERRM);
    RAISE;
END;
$$;

-- Usage: BEGIN; SELECT * FROM sp_p2p_transfer(1, 2, 3, 5, 1000.00); COMMIT;


-- ================================================================
-- PROCEDURE 3: sp_execute_trade_locked
-- Matches an OPEN buy order against an OPEN sell order.
-- FOR UPDATE on both orders prevents the same order being
-- matched by two concurrent sessions simultaneously.
-- ================================================================

CREATE OR REPLACE FUNCTION sp_execute_trade_locked(
    p_buy_order_id  INT,
    p_sell_order_id INT,
    OUT p_trade_id  INT,
    OUT p_success   BOOLEAN,
    OUT p_message   TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_buy_investor  INT;
    v_sell_investor INT;
    v_buy_asset     INT;
    v_sell_asset    INT;
    v_buy_units     INT;
    v_sell_units    INT;
    v_buy_status    VARCHAR(20);
    v_sell_status   VARCHAR(20);
    v_sell_price    NUMERIC(10,2);
    v_trade_units   INT;
    v_trade_total   NUMERIC(15,2);
    v_new_trade_id  INT;
    v_buyer_held    INT;
    v_buyer_bal     NUMERIC(15,2);
BEGIN
    -- Lock buy order row — blocks any other session trying to match it
    SELECT investor_id, asset_id, units, status
    INTO   v_buy_investor, v_buy_asset, v_buy_units, v_buy_status
    FROM   trade_order WHERE order_id = p_buy_order_id FOR UPDATE;

    -- Lock sell order row — same reason
    SELECT investor_id, asset_id, units, price, status
    INTO   v_sell_investor, v_sell_asset, v_sell_units, v_sell_price, v_sell_status
    FROM   trade_order WHERE order_id = p_sell_order_id FOR UPDATE;

    IF v_buy_investor IS NULL THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := FORMAT('Buy order #%s not found.', p_buy_order_id); RETURN;
    END IF;

    IF v_sell_investor IS NULL THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := FORMAT('Sell order #%s not found.', p_sell_order_id); RETURN;
    END IF;

    IF v_buy_status <> 'OPEN' THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := FORMAT('Buy order #%s is no longer OPEN. Status: %s',
                             p_buy_order_id, v_buy_status); RETURN;
    END IF;

    IF v_sell_status <> 'OPEN' THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := FORMAT('Sell order #%s is no longer OPEN. Status: %s',
                             p_sell_order_id, v_sell_status); RETURN;
    END IF;

    IF v_buy_asset <> v_sell_asset THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := 'Orders are for different assets.'; RETURN;
    END IF;

    -- Full match only (no partial fills)
    IF v_buy_units <> v_sell_units THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := FORMAT('Partial fills not supported. Buy units: %s, Sell units: %s',
                             v_buy_units, v_sell_units); RETURN;
    END IF;

    v_trade_units := v_buy_units;
    v_trade_total := v_trade_units * v_sell_price;

    -- Lock buyer wallet and check balance
    SELECT wallet_balance INTO v_buyer_bal
    FROM   investor WHERE investor_id = v_buy_investor FOR UPDATE;

    IF v_buyer_bal < v_trade_total THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := FORMAT('Buyer has insufficient funds. Required: %s, Available: %s',
                             v_trade_total, v_buyer_bal); RETURN;
    END IF;

    -- Generate new trade ID
    SELECT COALESCE(MAX(trade_id), 0) + 1 INTO v_new_trade_id FROM trade;

    -- 1. Insert trade record
    INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES (v_new_trade_id, v_sell_price, v_trade_units, CURRENT_DATE,
            p_buy_order_id, p_sell_order_id);

    -- 2. Update buyer ownership
    SELECT units_held INTO v_buyer_held
    FROM   ownership WHERE investor_id = v_buy_investor AND asset_id = v_buy_asset FOR UPDATE;

    IF v_buyer_held IS NULL THEN
        INSERT INTO ownership (investor_id, asset_id, units_held)
        VALUES (v_buy_investor, v_buy_asset, v_trade_units);
    ELSE
        UPDATE ownership SET units_held = units_held + v_trade_units
        WHERE investor_id = v_buy_investor AND asset_id = v_buy_asset;
    END IF;

    -- 3. Update seller ownership
    UPDATE ownership SET units_held = units_held - v_trade_units
    WHERE  investor_id = v_sell_investor AND asset_id = v_sell_asset;

    DELETE FROM ownership
    WHERE  investor_id = v_sell_investor AND asset_id = v_sell_asset AND units_held = 0;

    -- 4. Update order statuses
    UPDATE trade_order SET status = 'MATCHED' WHERE order_id = p_buy_order_id;
    UPDATE trade_order SET status = 'MATCHED' WHERE order_id = p_sell_order_id;

    -- 5. Wallet transactions (inside same transaction — rolls back if anything fails)
    INSERT INTO wallet_transaction
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (v_buy_investor, -v_trade_total, 'ASSET_PURCHASE', 'Trade Execution', NOW());

    INSERT INTO wallet_transaction
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (v_sell_investor, v_trade_total, 'ASSET_SALE', 'Trade Execution', NOW());

    p_trade_id := v_new_trade_id;
    p_success  := TRUE;
    p_message  := FORMAT(
        'Trade #%s executed: %s unit(s) of Asset #%s at %s each. Total: %s',
        v_new_trade_id, v_trade_units, v_buy_asset, v_sell_price, v_trade_total);

EXCEPTION WHEN OTHERS THEN
    p_success  := FALSE;
    p_trade_id := NULL;
    p_message  := FORMAT('Trade execution failed and rolled back: %s', SQLERRM);
    RAISE;
END;
$$;

-- Usage: BEGIN; SELECT * FROM sp_execute_trade_locked(3, 13); COMMIT;


-- ================================================================
-- TEST CASES
-- ================================================================

UPDATE investor SET wallet_balance = 50000 WHERE investor_id = 2;

-- Activate IPO #2 for today so the date check passes
UPDATE ipo
SET ipo_start_date = CURRENT_DATE,
    ipo_end_date   = CURRENT_DATE + INTERVAL '10 days'
WHERE ipo_id = 2;

BEGIN;
SELECT * FROM sp_subscribe_to_ipo(2, 2, 2);
COMMIT;

-- Verify
SELECT ipo_id, total_units, units_sold            FROM ipo       WHERE ipo_id = 2;
SELECT investor_id, investor_name, wallet_balance FROM investor  WHERE investor_id = 2;
SELECT investor_id, asset_id, units_held          FROM ownership WHERE investor_id = 2;


-- ================================================================
-- task6_transactions.sql
-- SECTION A: Valid multi-step transaction with COMMIT
-- ================================================================
-- Demonstrates a complete trade order + execution flowing through
-- multiple tables in one atomic block.
-- All steps succeed → COMMIT persists everything atomically.
-- ================================================================

BEGIN;

    -- Step 1: Place a buy order (investor 1, asset 2, 5 units at 1000)
    INSERT INTO trade_order (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
    VALUES (
        (SELECT COALESCE(MAX(order_id), 0) + 1 FROM trade_order),
        1, 2, 'BUY', 1000.00, 5, CURRENT_DATE, 'OPEN'
    );

    -- Step 2: Place a matching sell order (investor 4, asset 2, 5 units at 1000)
    INSERT INTO trade_order (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
    VALUES (
        (SELECT COALESCE(MAX(order_id), 0) + 1 FROM trade_order),
        4, 2, 'SELL', 1000.00, 5, CURRENT_DATE, 'OPEN'
    );

    -- Step 3: Insert the trade record linking both orders
    INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES (
        (SELECT COALESCE(MAX(trade_id), 0) + 1 FROM trade),
        1000.00, 5, CURRENT_DATE,
        (SELECT MAX(order_id) FROM trade_order WHERE investor_id = 1 AND order_type = 'BUY'  AND status = 'OPEN'),
        (SELECT MAX(order_id) FROM trade_order WHERE investor_id = 4 AND order_type = 'SELL' AND status = 'OPEN')
    );

    -- Step 4: Buyer gains 5 units
    INSERT INTO ownership (investor_id, asset_id, units_held)
    VALUES (1, 2, 5)
    ON CONFLICT (investor_id, asset_id)
    DO UPDATE SET units_held = ownership.units_held + 5;

    -- Step 5: Seller loses 5 units
    UPDATE ownership SET units_held = units_held - 5
    WHERE  investor_id = 4 AND asset_id = 2;

    -- Step 6: Mark both orders MATCHED
    UPDATE trade_order SET status = 'MATCHED'
    WHERE  investor_id = 1 AND order_type = 'BUY'  AND status = 'OPEN' AND asset_id = 2;

    UPDATE trade_order SET status = 'MATCHED'
    WHERE  investor_id = 4 AND order_type = 'SELL' AND status = 'OPEN' AND asset_id = 2;

-- All steps succeeded → commit atomically
COMMIT;
-- After COMMIT: trade exists, ownership updated, orders = MATCHED.


-- ================================================================
-- task6_transactions.sql
-- SECTION B: Invalid transaction with ROLLBACK
-- ================================================================
-- Attempts to deduct more units than the seller owns.
-- The units_held constraint catches it → ROLLBACK.
-- ================================================================

BEGIN;

    -- Attempt: deduct 9999 units from investor 1 on asset 2
    -- (investor 1 does not hold 9999 units — this will fail)
    UPDATE ownership
    SET    units_held = units_held - 9999
    WHERE  investor_id = 1 AND asset_id = 2;
    -- ^ Either violates CHECK constraint (units_held >= 0) or application detects negative

ROLLBACK;
-- After ROLLBACK: ownership is unchanged. Nothing was persisted.

-- Confirm state is unchanged:
SELECT investor_id, asset_id, units_held
FROM   ownership
WHERE  investor_id = 1 AND asset_id = 2;


-- ================================================================
-- task6_conflicts.sql
-- CONCURRENCY DEMONSTRATION
-- ================================================================
-- Open two browser tabs on Supabase SQL Editor to simulate
-- two concurrent sessions (Session A = Tab 1, Session B = Tab 2).
-- ================================================================


-- ================================================================
-- SCENARIO 1: Lost Update on Ownership
-- ================================================================
-- WITHOUT locking: both sessions read units_held = 10, both
-- subtract 5, both write 5 → result is 5 instead of correct 0.
-- ================================================================

-- BEFORE STATE setup (run once in either tab):
UPDATE ownership SET units_held = 10 WHERE investor_id = 5 AND asset_id = 6;
SELECT investor_id, asset_id, units_held AS "Before (should be 10)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;


-- ── WITHOUT FIX (shows anomaly) ───────────────────────────────────
-- Tab 1: Run BEGIN + SELECT, then pause
-- Tab 2: Run entire Session B block, then come back to Tab 1 and COMMIT

-- SESSION A (Tab 1):
BEGIN;
    SELECT units_held AS "Session A reads (sees 10)"
    FROM   ownership WHERE investor_id = 5 AND asset_id = 6;
    -- PAUSE here and run Session B in Tab 2
    UPDATE ownership SET units_held = units_held - 5
    WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;
-- Session A commits stale write (10-5 = 5, ignoring Session B's update)

-- SESSION B (Tab 2) — run while Session A is paused:
BEGIN;
    SELECT units_held AS "Session B reads (also sees 10)"
    FROM   ownership WHERE investor_id = 5 AND asset_id = 6;
    UPDATE ownership SET units_held = units_held - 5
    WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;
-- Session B commits 10-5 = 5

-- AFTER (anomaly): units_held = 5 instead of 0 — Session B's update was LOST
SELECT units_held AS "After WITHOUT fix (wrong: 5, should be 0)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;

-- WHY THIS IS A PROBLEM:
-- Investor 5 sold 5 units twice but only lost 5 in total.
-- They effectively retained 5 units they should no longer own.


-- ── WITH FIX: SELECT ... FOR UPDATE ───────────────────────────────
-- Reset state:
UPDATE ownership SET units_held = 10 WHERE investor_id = 5 AND asset_id = 6;

-- SESSION A (Tab 1) — with FOR UPDATE:
BEGIN;
    SELECT units_held FROM ownership
    WHERE  investor_id = 5 AND asset_id = 6
    FOR UPDATE;
    -- ^ Row is now exclusively locked. Session B BLOCKS at its FOR UPDATE.
    UPDATE ownership SET units_held = units_held - 5
    WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;
-- Session A commits → lock released → Session B unblocks

-- SESSION B (Tab 2) — run while Session A is paused:
BEGIN;
    SELECT units_held FROM ownership
    WHERE  investor_id = 5 AND asset_id = 6
    FOR UPDATE;
    -- ^ BLOCKS here until Session A commits, then reads 5 (not 10)
    UPDATE ownership SET units_held = units_held - 5
    WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;

-- AFTER (correct): units_held = 0
SELECT units_held AS "After WITH fix (correct: 0)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;


-- ================================================================
-- SCENARIO 2: Same Sell Order Executed Twice
-- ================================================================
-- WITHOUT locking: two sessions both see order as OPEN, both
-- execute a trade → duplicate trade rows, seller debited twice.
-- ================================================================

-- BEFORE STATE setup (run once):
UPDATE trade_order SET status = 'OPEN' WHERE order_id = 13;
SELECT order_id, status AS "Before (should be OPEN)"
FROM   trade_order WHERE order_id = 13;


-- ── WITHOUT FIX (shows anomaly) ───────────────────────────────────

-- SESSION A (Tab 1):
BEGIN;
    SELECT status FROM trade_order WHERE order_id = 13;
    -- Sees OPEN — inserts trade but pauses before COMMIT
    INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES ((SELECT COALESCE(MAX(trade_id),0)+1 FROM trade), 500.00, 5, CURRENT_DATE, 3, 13);
    -- PAUSE — run Session B now
COMMIT;

-- SESSION B (Tab 2) — run while Session A is paused:
BEGIN;
    SELECT status FROM trade_order WHERE order_id = 13;
    -- Also sees OPEN (Session A hasn't committed yet)
    INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES ((SELECT COALESCE(MAX(trade_id),0)+1 FROM trade), 500.00, 5, CURRENT_DATE, 3, 13);
COMMIT;

-- AFTER (anomaly): two trade rows for the same sell order
SELECT * FROM trade WHERE sell_order_id = 13 ORDER BY trade_id;
-- WHY THIS IS A PROBLEM:
-- The same units were sold twice. The seller's ownership was
-- debited twice — their units_held goes negative.


-- ── WITH FIX: FOR UPDATE + status re-check ────────────────────────
-- Reset state:
UPDATE trade_order SET status = 'OPEN' WHERE order_id = 13;
DELETE FROM trade WHERE sell_order_id = 13;

-- SESSION A (Tab 1) — with FOR UPDATE:
BEGIN;
    SELECT status FROM trade_order WHERE order_id = 13 FOR UPDATE;
    -- ^ Acquires exclusive row lock on the order
    -- Session B is now BLOCKED at its FOR UPDATE
    INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES ((SELECT COALESCE(MAX(trade_id),0)+1 FROM trade), 500.00, 5, CURRENT_DATE, 3, 13);
    UPDATE trade_order SET status = 'MATCHED' WHERE order_id = 13;
COMMIT;
-- Lock released. Session B unblocks and reads status = MATCHED.

-- SESSION B (Tab 2) — run while Session A is paused:
BEGIN;
    SELECT status FROM trade_order WHERE order_id = 13 FOR UPDATE;
    -- ^ BLOCKS until Session A commits, then unblocks and reads MATCHED
    -- Status check: order is no longer OPEN → refuse to proceed
    DO $$
    DECLARE v_status VARCHAR(20);
    BEGIN
        SELECT status INTO v_status FROM trade_order WHERE order_id = 13;
        IF v_status <> 'OPEN' THEN
            RAISE EXCEPTION 'Order 13 is no longer OPEN (status: %). Trade aborted.', v_status;
        END IF;
    END;
    $$;
ROLLBACK;
-- Session B rolls back cleanly — no duplicate trade inserted.

-- AFTER (correct): exactly one trade row, status = MATCHED
SELECT COUNT(*) AS "Trade rows (should be 1)" FROM trade WHERE sell_order_id = 13;
SELECT order_id, status AS "Order status (should be MATCHED)"
FROM   trade_order WHERE order_id = 13;
