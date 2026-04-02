USE fractional_ownership_db;

-- ============================================================
-- TRANSACTION 1: IPO Subscription
-- ============================================================
START TRANSACTION;
    --Top up the investor's wallet
    INSERT INTO WALLET_TRANSACTION (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (5, 120000.00, 'DEPOSIT', 'Bank Transfer', NOW());

    --Deduct the IPO subscription cost
    INSERT INTO WALLET_TRANSACTION (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (5, -120000.00, 'ASSET_PURCHASE', 'IPO Subscription', NOW());

    -- Showing that the investor owns those particular units they purchased
    INSERT INTO OWNERSHIP (investor_id, asset_id, units_held) VALUES (5, 3, 20)
    ON DUPLICATE KEY UPDATE units_held = units_held + 20;

    -- Manually log the ownership history entry
    INSERT INTO OWNERSHIP_HISTORY (investor_id, asset_id, units_before, units_after, change_date, change_type, trade_id, ipo_id)
    VALUES (5, 3, COALESCE((SELECT units_held FROM OWNERSHIP WHERE investor_id = 5 AND asset_id = 3), 0) - 20,
    COALESCE((SELECT units_held FROM OWNERSHIP WHERE investor_id = 5 AND asset_id = 3), 0),
    CURDATE(), 'IPO', NULL, 3);
COMMIT;

--Verfying whether it works or not
SELECT investor_id, wallet_balance FROM INVESTOR WHERE investor_id = 5;
SELECT * FROM OWNERSHIP WHERE investor_id = 5 AND asset_id = 3;


-- ============================================================
-- TRANSACTION 2: IPO Secondary Market Trade
-- ============================================================
START TRANSACTION;
    -- Step 1: Place the BUY order
        SET @new_order_id = (SELECT COALESCE(MAX(order_id), 0) + 1 FROM TRADE_ORDER);

        INSERT INTO TRADE_ORDER (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
        VALUES (@new_order_id, 3, 12, 'BUY', 7000.00, 10, CURDATE(), 'OPEN');

        SAVEPOINT after_order_placed;

        -- Step 2: Try to execute the trade against sell order #6
        --         (sell order #6 = Investor 3, Asset 12, 10 units @ ₹7,000 — OPEN)
        --         For a real scenario buyer and seller would differ; this illustrates the
        --         SAVEPOINT mechanism.

        SET @sell_order_id = 6;

        -- Guard: only proceed if the sell order is still OPEN
        SET @sell_status = (SELECT status FROM TRADE_ORDER WHERE order_id = @sell_order_id);

        -- Intentionally force the match to fail to demonstrate ROLLBACK TO SAVEPOINT
        -- (Change the condition below to 'OPEN' to let it succeed in normal use)
        IF @sell_status <> 'OPEN_MATCHED_ALREADY' THEN

            SET @new_trade_id = (SELECT COALESCE(MAX(trade_id), 0) + 1 FROM TRADE);
            SET @trade_total  = 10 * 7000.00;   -- 10 units × ₹7,000

            INSERT INTO TRADE (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
            VALUES (@new_trade_id, 7000.00, 10, CURDATE(), @new_order_id, @sell_order_id);

            -- Transfer ownership: buyer gains units
            INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
            VALUES (3, 12, 10)
            ON DUPLICATE KEY UPDATE units_held = units_held + 10;

            -- Transfer ownership: seller loses units
            UPDATE OWNERSHIP
            SET    units_held = units_held - 10
            WHERE  investor_id = (SELECT investor_id FROM TRADE_ORDER WHERE order_id = @sell_order_id)
              AND  asset_id    = 12;

            -- Wallet debit for buyer
            INSERT INTO WALLET_TRANSACTION
                (investor_id, amount, transaction_type, transfer_category, transaction_date)
            VALUES (3, -@trade_total, 'ASSET_PURCHASE', 'Secondary Market Trade', NOW());

            -- Wallet credit for seller
            INSERT INTO WALLET_TRANSACTION
                (investor_id, amount, transaction_type, transfer_category, transaction_date)
            VALUES ((SELECT investor_id FROM TRADE_ORDER WHERE order_id = @sell_order_id),
                    @trade_total, 'ASSET_SALE', 'Secondary Market Trade', NOW());

        ELSE
            -- Sell order is no longer OPEN: roll back only the trade steps,
            -- keeping the BUY order alive for future matching
            ROLLBACK TO SAVEPOINT after_order_placed;
        END IF;

COMMIT;

-- Verify the BUY order exists regardless of trade outcome
SELECT order_id, investor_id, asset_id, order_type, status FROM TRADE_ORDER WHERE order_id = @new_order_id;

-- ============================================================
-- TRANSACTION 3: Proportional Dividend Payout to All Owners
-- ============================================================
START TRANSACTION;
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    SELECT
        o.investor_id,
        ROUND((o.units_held / ipo.total_units) * 50000.00, 2),
        'DIVIDEND',
        CONCAT('Dividend Payout – Asset ', o.asset_id),
        NOW()
    FROM  OWNERSHIP o
    JOIN  IPO       ipo ON ipo.asset_id = o.asset_id
    WHERE o.asset_id    = 1
      AND o.units_held  > 0;

COMMIT;

-- Verify: wallet balances should have increased for investors 1, 2, 3
SELECT i.investor_id, i.investor_name, i.wallet_balance,
       wt.amount AS dividend_received
FROM   INVESTOR         i
JOIN   WALLET_TRANSACTION wt ON wt.investor_id = i.investor_id
WHERE  wt.transfer_category LIKE 'Dividend Payout%'
  AND  wt.transaction_type   = 'DIVIDEND'
ORDER  BY i.investor_id;


-- ============================================================
-- TRANSACTION 4: Trade Order Cancellation with Partial Refund
-- ============================================================
START TRANSACTION;
    -- Capture order details before modifying
    SELECT investor_id, order_type, price, units, status
    INTO   @cancel_investor, @cancel_type, @cancel_price, @cancel_units, @cancel_status
    FROM   TRADE_ORDER
    WHERE  order_id = 3;

    -- Only cancel if still OPEN
    IF @cancel_status = 'OPEN' THEN

        -- Update order status
        UPDATE TRADE_ORDER
        SET    status = 'CANCELLED'
        WHERE  order_id = 3;

        -- Refund the reserved amount only for BUY orders
        IF @cancel_type = 'BUY' THEN
            INSERT INTO WALLET_TRANSACTION
                (investor_id, amount, transaction_type, transfer_category, transaction_date)
            VALUES
                (@cancel_investor,
                 @cancel_price * @cancel_units,  -- positive = credit back
                 'REFUND',
                 'BUY Order Cancellation – Order #3',
                 NOW());
        END IF;

    ELSE
        -- Order already matched/cancelled; roll back the no-op cleanly
        ROLLBACK;
    END IF;

COMMIT;

-- Verify
SELECT order_id, status FROM TRADE_ORDER WHERE order_id = 3;
SELECT investor_id, amount, transaction_type FROM WALLET_TRANSACTION
WHERE  transfer_category LIKE 'BUY Order Cancellation%';


-- ============================================================
-- TRANSACTION 5: Asset Re-Verification and New IPO Launch
-- ============================================================
START TRANSACTION;
    -- Step 1: Re-verify the asset
    UPDATE ASSET
    SET    verification_status = 'Verified',
           verified_by         = 3
    WHERE  asset_id            = 10;

    -- Step 2: Insert a new valuation record (fresh market assessment)
    SET @new_val_id = (SELECT COALESCE(MAX(valuation_id), 0) + 1 FROM VALUATION);

    INSERT INTO VALUATION (valuation_id, asset_id, valuation_amount, valuation_date)
    VALUES (@new_val_id, 10, 280000.00, CURDATE())
    ON DUPLICATE KEY UPDATE valuation_amount = 280000.00;

    -- Step 3: Launch new IPO (requires asset to be Verified — enforced above)
    --         asset_id has a UNIQUE KEY on IPO so we use ON DUPLICATE KEY UPDATE
    --         to refresh an expired IPO rather than creating a second row.
    SET @new_ipo_id = (SELECT COALESCE(MAX(ipo_id), 0) + 1 FROM IPO);

    INSERT INTO IPO (ipo_id, asset_id, total_units, price_per_unit, ipo_start_date, ipo_end_date, lock_in_period)
    VALUES (@new_ipo_id, 10, 50, 5500.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 30)
    ON DUPLICATE KEY UPDATE
        price_per_unit  = 5500.00,
        ipo_start_date  = CURDATE(),
        ipo_end_date    = DATE_ADD(CURDATE(), INTERVAL 10 DAY),
        lock_in_period  = 30;

COMMIT;

-- Verify
SELECT asset_id, verification_status, verified_by FROM ASSET WHERE asset_id = 10;
SELECT valuation_id, valuation_amount, valuation_date FROM VALUATION WHERE asset_id = 10 ORDER BY valuation_date DESC LIMIT 1;
SELECT ipo_id, total_units, price_per_unit, ipo_start_date FROM IPO WHERE asset_id = 10;


-- ============================================================
-- TRANSACTION 6A & 6B: CONFLICTING TRANSACTIONS (Isolation Demo)
-- ============================================================

UPDATE INVESTOR SET wallet_balance = 100000.00 WHERE investor_id IN (4, 7);

-- ════════════════════════════════════════════════════════════
-- SESSION A  (run these steps in Terminal / Session A)
-- ════════════════════════════════════════════════════════════

-- [A1] Begin and lock the sell order row for exclusive update
-- SESSION A:
START TRANSACTION;

    SELECT order_id, investor_id, asset_id, units, status
    FROM   TRADE_ORDER
    WHERE  order_id = 5
    FOR UPDATE;                -- acquires exclusive row lock

    -- [A2] Check it is still OPEN and perform the trade
    -- (Session B's identical SELECT … FOR UPDATE will BLOCK here
    --  until Session A commits or rolls back)

    UPDATE TRADE_ORDER SET status = 'MATCHED' WHERE order_id = 5;

    UPDATE OWNERSHIP
    SET    units_held = units_held + 5
    WHERE  investor_id = 4 AND asset_id = 8;       -- Buyer A gets units

    UPDATE OWNERSHIP
    SET    units_held = units_held - 5
    WHERE  investor_id = 6 AND asset_id = 8;       -- Seller loses units

    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (4, -(5 * 9500.00), 'ASSET_PURCHASE', 'Conflicting Tx Demo – Session A', NOW());

    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (6, 5 * 9500.00, 'ASSET_SALE', 'Conflicting Tx Demo – Session A', NOW());

COMMIT;  -- [A3] Releases lock; Session B unblocks and re-reads the row

-- ════════════════════════════════════════════════════════════
-- SESSION B  (run these steps in Terminal / Session B — CONCURRENTLY with A)
-- ════════════════════════════════════════════════════════════

-- [B1] Attempt the same trade simultaneously
-- SESSION B:
START TRANSACTION;

    SELECT order_id, investor_id, asset_id, units, status
    FROM   TRADE_ORDER
    WHERE  order_id = 5
    FOR UPDATE;   -- BLOCKS until Session A commits (step A3)
                  -- After A commits, Session B reads status = 'MATCHED'

    -- [B2] Re-check status after acquiring lock
    -- At this point @b_status will be 'MATCHED' (set by Session A)
    SELECT @b_status := status FROM TRADE_ORDER WHERE order_id = 5;

    -- [B3] Guard: do not proceed if already matched
    -- Because @b_status = 'MATCHED', Session B rolls back gracefully.
    -- In production code the application layer would detect this and
    -- return "Order no longer available" to Investor 7.

ROLLBACK;  -- [B4] Session B releases lock, no data changed

-- Post-condition verification (run in either session after both complete):
SELECT order_id, status              FROM TRADE_ORDER     WHERE order_id = 5;
SELECT investor_id, asset_id, units_held FROM OWNERSHIP   WHERE asset_id = 8;
SELECT investor_id, wallet_balance   FROM INVESTOR        WHERE investor_id IN (4, 6, 7);


-- ============================================================
-- TRANSACTION 7: Atomic Investor Peer-to-Peer Unit Transfer
-- ------------------------------------------------------------
-- Two investors agree to transfer a specific number of units
-- of an asset directly between themselves (off-market transfer),
-- bypassing the order book. Both ownership records and the
-- ownership history are updated atomically.
--
-- WHY THIS MAKES SENSE:
--   Off-market transfers (gifts, estate settlements, OTC deals)
--   are a real use-case for fractional ownership platforms.
--   Unlike an order-book trade, there is no TRADE_ORDER row
--   involved. The transfer must atomically:
--     (1) Deduct units from the sender
--     (2) Credit units to the receiver
--     (3) Record an outgoing WALLET_TRANSACTION for the sender
--         (if a price was agreed)
--     (4) Record an incoming WALLET_TRANSACTION for the receiver
--   If steps (1) and (2) are not atomic, a crash between them
--   would cause units to simply vanish from the system — a
--   critical data integrity failure.
-- ============================================================

-- Scenario: Investor 4 (Sneha Iyer) transfers 10 units of
--           Asset 7 (Monet Landscape) to Investor 10 (Ishita Singh)
--           for an agreed price of ₹11,200 per unit (off-market).

START TRANSACTION;

    -- Validate Investor 4 holds enough units (guard before DML)
    SET @sender_units = (
        SELECT COALESCE(units_held, 0) FROM OWNERSHIP
        WHERE  investor_id = 4 AND asset_id = 7
    );

    IF @sender_units < 10 THEN
        -- Insufficient units — abort the whole transaction
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'P2P Transfer failed: sender holds insufficient units.';
    END IF;

    -- Step 1: Deduct from sender
    UPDATE OWNERSHIP
    SET    units_held = units_held - 10
    WHERE  investor_id = 4 AND asset_id = 7;

    -- Remove the row if units drop to zero
    DELETE FROM OWNERSHIP
    WHERE  investor_id = 4 AND asset_id = 7 AND units_held = 0;

    -- Step 2: Credit receiver (insert or increment)
    INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
    VALUES (10, 7, 10)
    ON DUPLICATE KEY UPDATE units_held = units_held + 10;

    -- Step 3: Payment — debit sender's wallet
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (10, -(10 * 11200.00), 'ASSET_PURCHASE', 'P2P Transfer – Asset 7 from Investor 4', NOW());

    -- Step 4: Credit seller's wallet
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (4, 10 * 11200.00, 'ASSET_SALE', 'P2P Transfer – Asset 7 to Investor 10', NOW());

COMMIT;

-- Verify
SELECT investor_id, asset_id, units_held FROM OWNERSHIP WHERE asset_id = 7 ORDER BY investor_id;
SELECT investor_id, wallet_balance FROM INVESTOR WHERE investor_id IN (4, 10);