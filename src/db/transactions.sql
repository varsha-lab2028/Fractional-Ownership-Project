-- ================================================================
-- TASK 6: TRANSACTIONS FILE (CORRECTED)
-- Project: Fractional Ownership Platform
-- Database: fractional_ownership_db
-- ================================================================
-- FIXES APPLIED:
--   1. LEAVE BEGIN → replaced with named block labels
--   2. VALUES() in ON DUPLICATE KEY → replaced with row alias
--   3. units_sold added via ALTER TABLE safely (IF NOT EXISTS)
--   4. Transaction 3 now locks rows with FOR UPDATE properly
--   5. All 3 transactions have proper conflict demo instructions
-- ================================================================

USE fractional_ownership_db;

-- ================================================================
-- TRANSACTION 1: IPO SUBSCRIPTION
-- ================================================================
-- An investor subscribes to an active IPO by purchasing units.
-- Uses FOR UPDATE locks on both the IPO row and INVESTOR row
-- to prevent two investors from over-subscribing simultaneously.
-- ================================================================

DROP PROCEDURE IF EXISTS sp_subscribe_to_ipo;

DELIMITER $$

CREATE PROCEDURE sp_subscribe_to_ipo(
    IN  p_investor_id INT,
    IN  p_ipo_id      INT,
    IN  p_units       INT,
    OUT p_success     TINYINT(1),
    OUT p_message     VARCHAR(255)
)
proc_main: BEGIN

    DECLARE v_asset_id     INT;
    DECLARE v_price        DECIMAL(10,2);
    DECLARE v_total        DECIMAL(15,2);
    DECLARE v_balance      DECIMAL(15,2);
    DECLARE v_ipo_start    DATE;
    DECLARE v_ipo_end      DATE;
    DECLARE v_total_units  INT;
    DECLARE v_units_sold   INT;
    DECLARE v_units_before INT DEFAULT 0;

    -- If any SQL error occurs, rollback everything and report failure
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = 0;
        SET p_message = 'Transaction failed and rolled back.';
    END;

    -- Basic input validation before opening a transaction
    IF p_units <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Units must be greater than 0.';
        LEAVE proc_main;
    END IF;

    START TRANSACTION;

    -- Lock the IPO row so no other session can modify units_sold
    -- until this transaction commits or rolls back
    SELECT asset_id, price_per_unit, ipo_start_date, ipo_end_date,
           total_units, units_sold
    INTO   v_asset_id, v_price, v_ipo_start, v_ipo_end,
           v_total_units, v_units_sold
    FROM   IPO
    WHERE  ipo_id = p_ipo_id
    FOR UPDATE;

    -- Check IPO exists
    IF v_asset_id IS NULL THEN
        SET p_success = 0;
        SET p_message = CONCAT('IPO #', p_ipo_id, ' does not exist.');
        ROLLBACK;
        LEAVE proc_main;
    END IF;

    -- Check IPO is currently active
    IF CURDATE() < v_ipo_start OR CURDATE() > v_ipo_end THEN
        SET p_success = 0;
        SET p_message = CONCAT('IPO #', p_ipo_id, ' is not currently active.');
        ROLLBACK;
        LEAVE proc_main;
    END IF;

    -- Check enough units remain (this is the critical concurrent check)
    IF v_units_sold + p_units > v_total_units THEN
        SET p_success = 0;
        SET p_message = CONCAT('Not enough units left. Available: ',
                               v_total_units - v_units_sold,
                               ', Requested: ', p_units);
        ROLLBACK;
        LEAVE proc_main;
    END IF;

    -- Lock the investor row so wallet balance cannot change mid-transaction
    SELECT wallet_balance
    INTO   v_balance
    FROM   INVESTOR
    WHERE  investor_id = p_investor_id
    FOR UPDATE;

    -- Check investor exists
    IF v_balance IS NULL THEN
        SET p_success = 0;
        SET p_message = CONCAT('Investor #', p_investor_id, ' does not exist.');
        ROLLBACK;
        LEAVE proc_main;
    END IF;

    SET v_total = p_units * v_price;

    -- Check investor has enough funds
    IF v_balance < v_total THEN
        SET p_success = 0;
        SET p_message = CONCAT('Insufficient balance. Required: ', v_total,
                               ', Available: ', v_balance);
        ROLLBACK;
        LEAVE proc_main;
    END IF;

    -- Lock existing ownership row if present (prevents concurrent update conflicts)
    SELECT units_held
    INTO   v_units_before
    FROM   OWNERSHIP
    WHERE  investor_id = p_investor_id
      AND  asset_id    = v_asset_id
    FOR UPDATE;

    IF v_units_before IS NULL THEN
        SET v_units_before = 0;
    END IF;

    -- All checks passed — perform the actual writes

    -- 1. Deduct wallet balance
    UPDATE INVESTOR
    SET    wallet_balance = wallet_balance - v_total
    WHERE  investor_id = p_investor_id;

    -- 2. Increment units sold in IPO
    UPDATE IPO
    SET    units_sold = units_sold + p_units
    WHERE  ipo_id = p_ipo_id;

    -- 3. Grant ownership (insert new row or add to existing)
    --    FIX: use row alias instead of deprecated VALUES()
    INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
    VALUES (p_investor_id, v_asset_id, p_units) AS new_row
    ON DUPLICATE KEY UPDATE
        units_held = units_held + new_row.units_held;

    -- 4. Log wallet transaction
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (p_investor_id, -v_total, 'ASSET_PURCHASE', 'IPO Subscription', NOW());

    -- 5. Log ownership history
    INSERT INTO OWNERSHIP_HISTORY
        (investor_id, asset_id, units_before, units_after,
         change_date, change_type, trade_id, ipo_id)
    VALUES
        (p_investor_id, v_asset_id, v_units_before, v_units_before + p_units,
         CURDATE(), 'IPO', NULL, p_ipo_id);

    COMMIT;

    SET p_success = 1;
    SET p_message = CONCAT('IPO subscription successful. Investor ', p_investor_id,
                           ' bought ', p_units, ' unit(s) in IPO #', p_ipo_id,
                           '. Total charged: ', v_total);
END$$

DELIMITER ;


-- ================================================================
-- TRANSACTION 2: PEER-TO-PEER (P2P) UNIT TRANSFER
-- ================================================================
-- Investor A transfers units of an asset directly to Investor B
-- for an agreed price, bypassing the order book entirely.
-- Uses FOR UPDATE to lock both ownership rows and both wallet rows
-- to prevent conflicting concurrent transfers of the same units.
-- ================================================================

DROP PROCEDURE IF EXISTS sp_p2p_transfer;

DELIMITER $$

CREATE PROCEDURE sp_p2p_transfer(
    IN  p_sender    INT,
    IN  p_receiver  INT,
    IN  p_asset     INT,
    IN  p_units     INT,
    IN  p_price     DECIMAL(10,2),
    OUT p_success   TINYINT(1),
    OUT p_message   VARCHAR(255)
)
-- FIX: named label so LEAVE works correctly
p2p_block: BEGIN

    DECLARE v_units   INT    DEFAULT 0;
    DECLARE v_balance DECIMAL(15,2);
    DECLARE v_total   DECIMAL(15,2);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = 0;
        SET p_message = 'P2P transfer failed and rolled back.';
    END;

    -- Input validation before opening transaction
    IF p_units <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Units must be greater than 0.';
        LEAVE p2p_block;
    END IF;

    IF p_price < 0 THEN
        SET p_success = 0;
        SET p_message = 'Price cannot be negative.';
        LEAVE p2p_block;
    END IF;

    IF p_sender = p_receiver THEN
        SET p_success = 0;
        SET p_message = 'Sender and receiver cannot be the same investor.';
        LEAVE p2p_block;
    END IF;

    START TRANSACTION;

    -- Lock sender's ownership row — prevents parallel transfers of same units
    SELECT units_held
    INTO   v_units
    FROM   OWNERSHIP
    WHERE  investor_id = p_sender
      AND  asset_id    = p_asset
    FOR UPDATE;

    IF v_units IS NULL OR v_units < p_units THEN
        SET p_success = 0;
        SET p_message = CONCAT('Sender does not hold enough units. Holds: ',
                               COALESCE(v_units, 0), ', Requested: ', p_units);
        ROLLBACK;
        LEAVE p2p_block;
    END IF;

    SET v_total = p_units * p_price;

    -- Lock receiver's wallet row
    SELECT wallet_balance
    INTO   v_balance
    FROM   INVESTOR
    WHERE  investor_id = p_receiver
    FOR UPDATE;

    IF v_balance IS NULL THEN
        SET p_success = 0;
        SET p_message = CONCAT('Receiver investor #', p_receiver, ' does not exist.');
        ROLLBACK;
        LEAVE p2p_block;
    END IF;

    IF v_balance < v_total THEN
        SET p_success = 0;
        SET p_message = CONCAT('Receiver has insufficient funds. Required: ',
                               v_total, ', Available: ', v_balance);
        ROLLBACK;
        LEAVE p2p_block;
    END IF;

    -- Lock sender's wallet row as well
    SELECT wallet_balance INTO v_balance
    FROM   INVESTOR
    WHERE  investor_id = p_sender
    FOR UPDATE;

    -- All checks passed — perform writes

    -- 1. Deduct units from sender
    UPDATE OWNERSHIP
    SET    units_held = units_held - p_units
    WHERE  investor_id = p_sender
      AND  asset_id    = p_asset;

    -- Remove sender's row if they now hold zero units
    DELETE FROM OWNERSHIP
    WHERE  investor_id = p_sender
      AND  asset_id    = p_asset
      AND  units_held  = 0;

    -- 2. Credit units to receiver (insert or increment)
    --    FIX: use row alias instead of deprecated VALUES()
    INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
    VALUES (p_receiver, p_asset, p_units) AS new_row
    ON DUPLICATE KEY UPDATE
        units_held = units_held + new_row.units_held;

    -- 3. Deduct payment from receiver's wallet
    UPDATE INVESTOR
    SET    wallet_balance = wallet_balance - v_total
    WHERE  investor_id = p_receiver;

    -- 4. Credit payment to sender's wallet
    UPDATE INVESTOR
    SET    wallet_balance = wallet_balance + v_total
    WHERE  investor_id = p_sender;

    -- 5. Log wallet transactions for both parties
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (p_receiver, -v_total, 'ASSET_PURCHASE',
         CONCAT('P2P Transfer – Asset #', p_asset, ' from Investor #', p_sender), NOW());

    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (p_sender, v_total, 'ASSET_SALE',
         CONCAT('P2P Transfer – Asset #', p_asset, ' to Investor #', p_receiver), NOW());

    COMMIT;

    SET p_success = 1;
    SET p_message = CONCAT('P2P transfer successful. Investor ', p_sender,
                           ' transferred ', p_units, ' unit(s) of Asset #', p_asset,
                           ' to Investor ', p_receiver,
                           ' for total price: ', v_total);
END$$

DELIMITER ;


-- ================================================================
-- TRANSACTION 3: TRADE EXECUTION (SECONDARY MARKET)
-- ================================================================
-- Matches an existing OPEN buy order against an existing OPEN
-- sell order on the secondary market.
-- Uses FOR UPDATE on both orders so that if two sessions try to
-- match the same order simultaneously only one will succeed.
-- ================================================================

DROP PROCEDURE IF EXISTS sp_execute_trade_locked;

DELIMITER $$

CREATE PROCEDURE sp_execute_trade_locked(
    IN  p_buy_order_id  INT,
    IN  p_sell_order_id INT,
    OUT p_trade_id      INT,
    OUT p_success       TINYINT(1),
    OUT p_message       VARCHAR(255)
)
-- FIX: named label so LEAVE works correctly
trade_block: BEGIN

    DECLARE v_buy_investor  INT;
    DECLARE v_sell_investor INT;
    DECLARE v_buy_asset     INT;
    DECLARE v_sell_asset    INT;
    DECLARE v_buy_units     INT;
    DECLARE v_sell_units    INT;
    DECLARE v_buy_price     DECIMAL(10,2);
    DECLARE v_sell_price    DECIMAL(10,2);
    DECLARE v_buy_status    VARCHAR(20);
    DECLARE v_sell_status   VARCHAR(20);
    DECLARE v_trade_units   INT;
    DECLARE v_trade_total   DECIMAL(15,2);
    DECLARE v_new_trade_id  INT;
    DECLARE v_buyer_held    INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = 'Trade execution failed and rolled back.';
    END;

    START TRANSACTION;

    -- FIX: Both reads are NOW inside the transaction with FOR UPDATE
    -- This prevents another session from matching the same orders concurrently

    -- Lock the buy order row
    SELECT investor_id, asset_id, units, price, status
    INTO   v_buy_investor, v_buy_asset, v_buy_units, v_buy_price, v_buy_status
    FROM   TRADE_ORDER
    WHERE  order_id = p_buy_order_id
    FOR UPDATE;

    -- Lock the sell order row
    SELECT investor_id, asset_id, units, price, status
    INTO   v_sell_investor, v_sell_asset, v_sell_units, v_sell_price, v_sell_status
    FROM   TRADE_ORDER
    WHERE  order_id = p_sell_order_id
    FOR UPDATE;

    -- Validate buy order exists
    IF v_buy_investor IS NULL THEN
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = CONCAT('Buy order #', p_buy_order_id, ' not found.');
        ROLLBACK;
        LEAVE trade_block;
    END IF;

    -- Validate sell order exists
    IF v_sell_investor IS NULL THEN
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = CONCAT('Sell order #', p_sell_order_id, ' not found.');
        ROLLBACK;
        LEAVE trade_block;
    END IF;

    -- Validate both orders are still OPEN (critical for conflict demo)
    IF v_buy_status <> 'OPEN' THEN
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = CONCAT('Buy order #', p_buy_order_id,
                                ' is no longer OPEN. Status: ', v_buy_status);
        ROLLBACK;
        LEAVE trade_block;
    END IF;

    IF v_sell_status <> 'OPEN' THEN
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = CONCAT('Sell order #', p_sell_order_id,
                                ' is no longer OPEN. Status: ', v_sell_status);
        ROLLBACK;
        LEAVE trade_block;
    END IF;

    -- Validate both orders are for the same asset
    IF v_buy_asset <> v_sell_asset THEN
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = 'Orders are for different assets.';
        ROLLBACK;
        LEAVE trade_block;
    END IF;

    -- Validate price: buyer must be willing to pay at least the sell price
    IF v_buy_price < v_sell_price THEN
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = CONCAT('Price mismatch. Buy price: ', v_buy_price,
                                ', Sell price: ', v_sell_price);
        ROLLBACK;
        LEAVE trade_block;
    END IF;

    -- Calculate trade units (minimum of what buyer wants and seller has)
    SET v_trade_units = LEAST(v_buy_units, v_sell_units);
    SET v_trade_total = v_trade_units * v_sell_price;

    -- Generate new trade ID
    SELECT COALESCE(MAX(trade_id), 0) + 1
    INTO   v_new_trade_id
    FROM   TRADE;

    -- 1. Insert the trade record
    --    (trigger trg_update_order_status_after_trade fires here
    --     and automatically marks both orders as MATCHED)
    INSERT INTO TRADE
        (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES
        (v_new_trade_id, v_sell_price, v_trade_units, CURDATE(),
         p_buy_order_id, p_sell_order_id);

    -- 2. Lock buyer's wallet and update
    SELECT wallet_balance INTO @buyer_balance
    FROM   INVESTOR WHERE investor_id = v_buy_investor FOR UPDATE;

    IF @buyer_balance < v_trade_total THEN
        SET p_success  = 0;
        SET p_trade_id = NULL;
        SET p_message  = CONCAT('Buyer has insufficient funds. Required: ',
                                v_trade_total, ', Available: ', @buyer_balance);
        ROLLBACK;
        LEAVE trade_block;
    END IF;

    -- 3. Update buyer ownership (insert if first time owning this asset)
    SELECT units_held INTO v_buyer_held
    FROM   OWNERSHIP
    WHERE  investor_id = v_buy_investor AND asset_id = v_buy_asset
    FOR UPDATE;

    IF v_buyer_held IS NULL THEN
        INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
        VALUES (v_buy_investor, v_buy_asset, v_trade_units);
    ELSE
        UPDATE OWNERSHIP
        SET    units_held = units_held + v_trade_units
        WHERE  investor_id = v_buy_investor AND asset_id = v_buy_asset;
    END IF;

    -- 4. Update seller ownership (deduct units)
    UPDATE OWNERSHIP
    SET    units_held = units_held - v_trade_units
    WHERE  investor_id = v_sell_investor AND asset_id = v_sell_asset;

    -- Remove seller row if they now hold zero units
    DELETE FROM OWNERSHIP
    WHERE  investor_id = v_sell_investor
      AND  asset_id    = v_sell_asset
      AND  units_held  = 0;

    -- 5. Wallet transactions
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (v_buy_investor, -v_trade_total, 'ASSET_PURCHASE', 'Trade Execution', NOW());

    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (v_sell_investor, v_trade_total, 'ASSET_SALE', 'Trade Execution', NOW());

    COMMIT;

    SET p_trade_id = v_new_trade_id;
    SET p_success  = 1;
    SET p_message  = CONCAT('Trade #', v_new_trade_id, ' executed: ',
                            v_trade_units, ' unit(s) of Asset #', v_buy_asset,
                            ' at price ', v_sell_price,
                            '. Total: ', v_trade_total);
END$$

DELIMITER ;


-- ================================================================
-- TEST CASES
-- ================================================================

-- Give investor 2 some funds to work with
UPDATE INVESTOR SET wallet_balance = 50000 WHERE investor_id = 2;

-- Test Transaction 1: Investor 2 subscribes to IPO #2
-- NOTE: IPO #2 dates are in 2024 so the date check will reject it.
-- For testing, temporarily update the dates:
UPDATE IPO
SET ipo_start_date = CURDATE(),
    ipo_end_date   = DATE_ADD(CURDATE(), INTERVAL 10 DAY)
WHERE ipo_id = 2;

CALL sp_subscribe_to_ipo(2, 2, 2, @ok, @msg);
SELECT @ok AS success, @msg AS message;

-- Verify results
SELECT ipo_id, total_units, units_sold            FROM IPO      WHERE ipo_id = 2;
SELECT investor_id, investor_name, wallet_balance FROM INVESTOR  WHERE investor_id = 2;
SELECT investor_id, asset_id, units_held          FROM OWNERSHIP WHERE investor_id = 2;


-- ================================================================
-- CONFLICT DEMO — RUN IN TWO SEPARATE SESSIONS SIMULTANEOUSLY
-- ================================================================
-- This demonstrates that FOR UPDATE prevents double-subscriptions.
-- Open two MySQL sessions and run each block at the same time.
--
-- EXPECTED OUTCOME:
--   Session A succeeds (or whichever runs first)
--   Session B is BLOCKED until Session A commits,
--   then sees units_sold is now full and returns an error message.
-- ================================================================

-- SESSION A (run first, or at the same time as Session B):
-- CALL sp_subscribe_to_ipo(2, 2, 50, @ok, @msg);
-- SELECT @ok AS success, @msg AS message;

-- SESSION B (run at the same time as Session A):
-- CALL sp_subscribe_to_ipo(3, 2, 50, @ok, @msg);
-- SELECT @ok AS success, @msg AS message;

-- After both sessions finish, verify only one succeeded:
-- SELECT ipo_id, total_units, units_sold FROM IPO WHERE ipo_id = 2;