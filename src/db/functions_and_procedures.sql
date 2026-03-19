USE test;

-- Functions

-- ------------------------------------------------------------
-- FUNCTION 1: fn_get_wallet_balance
-- ------------------------------------------------------------
-- Returns the current wallet balance for a given investor.
-- Can be used anywhere a numeric expression is expected.

DROP FUNCTION IF EXISTS fn_get_wallet_balance;

DELIMITER $$
CREATE FUNCTION fn_get_wallet_balance(p_investor_id INT)
RETURNS DECIMAL(15, 2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_balance DECIMAL(15, 2) DEFAULT 0.00;
    SELECT wallet_balance
    INTO   v_balance
    FROM   INVESTOR
    WHERE  investor_id = p_investor_id;

    RETURN COALESCE(v_balance, 0.00);
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- FUNCTION 2: fn_get_latest_valuation
-- ------------------------------------------------------------
-- Returns the most recent valuation amount for a given asset.
-- Returns 0.00 if no valuation record exists.

DROP FUNCTION IF EXISTS fn_get_latest_valuation;

DELIMITER $$
CREATE FUNCTION fn_get_latest_valuation(p_asset_id INT)
RETURNS DECIMAL(12, 2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_amount DECIMAL(12, 2) DEFAULT 0.00;
    SELECT valuation_amount
    INTO   v_amount
    FROM   VALUATION
    WHERE  asset_id     = p_asset_id
      AND  valuation_date = (
               SELECT MAX(valuation_date)
               FROM   VALUATION
               WHERE  asset_id = p_asset_id
           )
    LIMIT 1;

    RETURN COALESCE(v_amount, 0.00);
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- FUNCTION 3: fn_get_total_units_held
-- ------------------------------------------------------------
-- Returns the total number of units held across ALL investors
-- for a specific asset (sum of all OWNERSHIP rows).

DROP FUNCTION IF EXISTS fn_get_total_units_held;

DELIMITER $$
CREATE FUNCTION fn_get_total_units_held(p_asset_id INT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total INT DEFAULT 0;

    SELECT COALESCE(SUM(units_held), 0)
    INTO   v_total
    FROM   OWNERSHIP
    WHERE  asset_id = p_asset_id;

    RETURN v_total;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- FUNCTION 4: fn_get_investor_portfolio_value
-- ------------------------------------------------------------
-- Returns the current market value of an investor's portfolio.
-- Each holding is valued at:
--   units_held * (latest valuation / total_units from IPO)
-- Uses: OWNERSHIP JOIN IPO JOIN VALUATION
-- Example: SELECT fn_get_investor_portfolio_value(1);
-- ------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_get_investor_portfolio_value;

DELIMITER $$
CREATE FUNCTION fn_get_investor_portfolio_value(p_investor_id INT)
RETURNS DECIMAL(15, 2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_portfolio_value DECIMAL(15, 2) DEFAULT 0.00;

    SELECT COALESCE(
               SUM(
                   (o.units_held * 1.0 / i.total_units)
                   * fn_get_latest_valuation(o.asset_id)
               ),
               0.00
           )
    INTO   v_portfolio_value
    FROM   OWNERSHIP o
    JOIN   IPO       i ON i.asset_id = o.asset_id
    WHERE  o.investor_id = p_investor_id;

    RETURN v_portfolio_value;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- FUNCTION 5: fn_count_investor_holdings
-- ------------------------------------------------------------
-- Returns the number of distinct assets an investor holds.
-- Useful for dashboard summaries and risk diversification checks.
-- Example: SELECT fn_count_investor_holdings(1);
-- ------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_count_investor_holdings;

DELIMITER $$
CREATE FUNCTION fn_count_investor_holdings(p_investor_id INT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO   v_count
    FROM   OWNERSHIP
    WHERE  investor_id = p_investor_id
      AND  units_held  > 0;

    RETURN v_count;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- FUNCTION 6: fn_get_total_trade_volume
-- ------------------------------------------------------------
-- Returns the total number of units ever traded for an asset
-- across all completed trades (from the TRADE table).
-- Example: SELECT fn_get_total_trade_volume(1);
-- ------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_get_total_trade_volume;

DELIMITER $$
CREATE FUNCTION fn_get_total_trade_volume(p_asset_id INT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_volume INT DEFAULT 0;

    SELECT COALESCE(SUM(t.trade_units), 0)
    INTO   v_volume
    FROM   TRADE       t
    JOIN   TRADE_ORDER o ON o.order_id = t.buy_order_id
    WHERE  o.asset_id = p_asset_id;

    RETURN v_volume;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- FUNCTION 7: fn_investor_has_sufficient_balance
-- ------------------------------------------------------------
-- Returns 1 (TRUE) if an investor has at least p_required_amount
-- in their wallet, 0 (FALSE) otherwise.
-- Used for pre-trade checks.
-- Example: SELECT fn_investor_has_sufficient_balance(1, 5000.00);
-- ------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_investor_has_sufficient_balance;

DELIMITER $$
CREATE FUNCTION fn_investor_has_sufficient_balance(
    p_investor_id    INT,
    p_required_amount DECIMAL(15, 2)
)
RETURNS TINYINT(1)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_balance DECIMAL(15, 2) DEFAULT 0.00;

    SET v_balance = fn_get_wallet_balance(p_investor_id);

    IF v_balance >= p_required_amount THEN
        RETURN 1;
    ELSE
        RETURN 0;
    END IF;
END$$
DELIMITER ;


-- Procedures

-- ------------------------------------------------------------
-- PROCEDURE 1: sp_get_investor_holdings
-- ------------------------------------------------------------
-- Returns a result set of all assets held by a given investor,
-- enriched with asset name, category, latest valuation, and
-- current holding value.
-- Equivalent to a SQL table function over OWNERSHIP + ASSET + VALUATION.
-- CALL sp_get_investor_holdings(1);
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_get_investor_holdings;

DELIMITER $$
CREATE PROCEDURE sp_get_investor_holdings(IN p_investor_id INT)
BEGIN
    SELECT
        o.investor_id,
        a.asset_id,
        a.asset_name                              AS asset_name,
        a.category,
        o.units_held,
        i.price_per_unit                    AS ipo_price_per_unit,
        fn_get_latest_valuation(a.asset_id) AS latest_valuation,
        ROUND(
            (o.units_held * 1.0 / i.total_units)
            * fn_get_latest_valuation(a.asset_id),
            2
        )                                   AS current_holding_value
    FROM  OWNERSHIP o
    JOIN  ASSET     a ON a.asset_id = o.asset_id
    JOIN  IPO       i ON i.asset_id = o.asset_id
    WHERE o.investor_id = p_investor_id
      AND o.units_held  > 0
    ORDER BY current_holding_value DESC;
END$$
DELIMITER ;


-- ============================================================
-- SECTION 3: STORED PROCEDURES (business logic in the DB)
-- ============================================================


-- ------------------------------------------------------------
-- PROCEDURE 2: sp_deposit_to_wallet
-- ------------------------------------------------------------
-- Deposits a positive amount into an investor's wallet by
-- inserting into WALLET_TRANSACTION.
-- The AFTER INSERT trigger (trg_update_wallet_balance_after_transaction)
-- automatically updates INVESTOR.wallet_balance.
-- OUT p_success: 1 on success, 0 on failure.
-- OUT p_message: descriptive result message.
--
-- CALL sp_deposit_to_wallet(1, 5000.00, 'Bank Transfer', @ok, @msg);
-- SELECT @ok, @msg;
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_deposit_to_wallet;

DELIMITER $$
CREATE PROCEDURE sp_deposit_to_wallet(
    IN  p_investor_id INT,
    IN  p_amount      DECIMAL(15, 2),
    IN  p_category    VARCHAR(100),
    OUT p_success     TINYINT(1),
    OUT p_message     VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success = 0;
        ROLLBACK;
    END;

    -- Validate inputs
    IF p_amount <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Deposit amount must be greater than zero.';
        LEAVE sp_deposit_to_wallet;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM INVESTOR WHERE investor_id = p_investor_id) THEN
        SET p_success = 0;
        SET p_message = CONCAT('Investor ID ', p_investor_id, ' does not exist.');
        LEAVE sp_deposit_to_wallet;
    END IF;

    START TRANSACTION;

    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (p_investor_id, p_amount, 'DEPOSIT', p_category, NOW());

    COMMIT;

    SET p_success = 1;
    SET p_message = CONCAT('Successfully deposited $', p_amount,
                            ' to investor ', p_investor_id, '.');
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 3: sp_withdraw_from_wallet
-- ------------------------------------------------------------
-- Withdraws an amount from an investor's wallet.
-- The BEFORE INSERT trigger (trg_prevent_negative_wallet_balance)
-- will automatically block the withdrawal if it would cause
-- a negative balance.
-- OUT p_success: 1 on success, 0 on failure.
-- OUT p_message: descriptive result message.
--
-- CALL sp_withdraw_from_wallet(1, 1000.00, 'ATM Withdrawal', @ok, @msg);
-- SELECT @ok, @msg;
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_withdraw_from_wallet;

DELIMITER $$
CREATE PROCEDURE sp_withdraw_from_wallet(
    IN  p_investor_id INT,
    IN  p_amount      DECIMAL(15, 2),
    IN  p_category    VARCHAR(100),
    OUT p_success     TINYINT(1),
    OUT p_message     VARCHAR(255)
)
BEGIN
    DECLARE v_balance DECIMAL(15, 2);

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success = 0;
        ROLLBACK;
    END;

    IF p_amount <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Withdrawal amount must be greater than zero.';
        LEAVE sp_withdraw_from_wallet;
    END IF;

    -- Check sufficient balance before attempting
    SET v_balance = fn_get_wallet_balance(p_investor_id);
    IF v_balance < p_amount THEN
        SET p_success = 0;
        SET p_message = CONCAT('Insufficient balance. Available: $', v_balance,
                                ', Required: $', p_amount);
        LEAVE sp_withdraw_from_wallet;
    END IF;

    START TRANSACTION;

    -- Amount is stored as NEGATIVE for debits
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (p_investor_id, -p_amount, 'WITHDRAWAL', p_category, NOW());

    COMMIT;

    SET p_success = 1;
    SET p_message = CONCAT('Successfully withdrew $', p_amount,
                            ' from investor ', p_investor_id, '.');
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 4: sp_execute_trade
-- ------------------------------------------------------------
-- Matches a buy order with a sell order and settles the trade.
-- Steps performed (all inside one transaction):
--   1. Validate both orders are OPEN and for the same asset.
--   2. Determine trade units (min of buy/sell units).
--   3. Insert into TRADE.
--   4. Update OWNERSHIP for buyer (+units) and seller (-units).
--   5. Deduct cost from buyer's wallet; credit seller's wallet.
--   6. Mark both orders as MATCHED.
-- The ownership triggers auto-log to OWNERSHIP_HISTORY.
-- The order-status trigger (trg_update_order_status_after_trade)
-- also fires on TRADE INSERT to mark orders MATCHED.
--
-- CALL sp_execute_trade(3, 5, @tid, @ok, @msg);
-- SELECT @tid, @ok, @msg;
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_execute_trade;

DELIMITER $$
CREATE PROCEDURE sp_execute_trade(
    IN  p_buy_order_id  INT,
    IN  p_sell_order_id INT,
    OUT p_trade_id      INT,
    OUT p_success       TINYINT(1),
    OUT p_message       VARCHAR(255)
)
BEGIN
    DECLARE v_buy_investor_id  INT;
    DECLARE v_sell_investor_id INT;
    DECLARE v_buy_asset_id     INT;
    DECLARE v_sell_asset_id    INT;
    DECLARE v_buy_units        INT;
    DECLARE v_sell_units       INT;
    DECLARE v_buy_status       VARCHAR(20);
    DECLARE v_sell_status      VARCHAR(20);
    DECLARE v_sell_price       DECIMAL(10, 2);
    DECLARE v_trade_units      INT;
    DECLARE v_trade_total      DECIMAL(15, 2);
    DECLARE v_buyer_prev_units  INT DEFAULT 0;
    DECLARE v_seller_prev_units INT DEFAULT 0;
    DECLARE v_new_trade_id     INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success  = 0;
        SET p_trade_id = NULL;
        ROLLBACK;
    END;

    -- ── Load buy order ────────────────────────────────────────
    SELECT investor_id, asset_id, units, status
    INTO   v_buy_investor_id, v_buy_asset_id, v_buy_units, v_buy_status
    FROM   TRADE_ORDER
    WHERE  order_id = p_buy_order_id;

    IF v_buy_investor_id IS NULL THEN
        SET p_success = 0;
        SET p_message = CONCAT('Buy order #', p_buy_order_id, ' not found.');
        LEAVE sp_execute_trade;
    END IF;

    IF v_buy_status <> 'OPEN' THEN
        SET p_success = 0;
        SET p_message = CONCAT('Buy order #', p_buy_order_id,
                                ' is not OPEN (status: ', v_buy_status, ').');
        LEAVE sp_execute_trade;
    END IF;

    -- ── Load sell order ───────────────────────────────────────
    SELECT investor_id, asset_id, units, price, status
    INTO   v_sell_investor_id, v_sell_asset_id, v_sell_units, v_sell_price, v_sell_status
    FROM   TRADE_ORDER
    WHERE  order_id = p_sell_order_id;

    IF v_sell_investor_id IS NULL THEN
        SET p_success = 0;
        SET p_message = CONCAT('Sell order #', p_sell_order_id, ' not found.');
        LEAVE sp_execute_trade;
    END IF;

    IF v_sell_status <> 'OPEN' THEN
        SET p_success = 0;
        SET p_message = CONCAT('Sell order #', p_sell_order_id,
                                ' is not OPEN (status: ', v_sell_status, ').');
        LEAVE sp_execute_trade;
    END IF;

    -- ── Validate same asset ───────────────────────────────────
    IF v_buy_asset_id <> v_sell_asset_id THEN
        SET p_success = 0;
        SET p_message = 'Orders are for different assets — cannot match.';
        LEAVE sp_execute_trade;
    END IF;

    -- ── Calculate trade details ───────────────────────────────
    SET v_trade_units = LEAST(v_buy_units, v_sell_units);
    SET v_trade_total = v_trade_units * v_sell_price;

    -- ── Check buyer's wallet ──────────────────────────────────
    IF fn_investor_has_sufficient_balance(v_buy_investor_id, v_trade_total) = 0 THEN
        SET p_success = 0;
        SET p_message = CONCAT('Buyer (investor ', v_buy_investor_id,
                                ') has insufficient wallet balance for this trade.');
        LEAVE sp_execute_trade;
    END IF;

    -- ── Get next trade ID ─────────────────────────────────────
    SELECT COALESCE(MAX(trade_id), 0) + 1
    INTO   v_new_trade_id
    FROM   TRADE;

    START TRANSACTION;

    -- ── 1. Insert the trade ───────────────────────────────────
    -- (trg_update_order_status_after_trade fires here and marks orders MATCHED)
    INSERT INTO TRADE (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES (v_new_trade_id, v_sell_price, v_trade_units, CURDATE(), p_buy_order_id, p_sell_order_id);

    -- ── 2. Update buyer's ownership ───────────────────────────
    SELECT COALESCE(units_held, 0)
    INTO   v_buyer_prev_units
    FROM   OWNERSHIP
    WHERE  investor_id = v_buy_investor_id AND asset_id = v_buy_asset_id;

    IF v_buyer_prev_units IS NULL OR NOT EXISTS (
        SELECT 1 FROM OWNERSHIP
        WHERE investor_id = v_buy_investor_id AND asset_id = v_buy_asset_id
    ) THEN
        -- First time holding this asset — INSERT (trg_log_ownership_history_on_insert fires)
        INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
        VALUES (v_buy_investor_id, v_buy_asset_id, v_trade_units);
    ELSE
        -- Update existing holding (trg_log_ownership_history_on_update fires)
        UPDATE OWNERSHIP
        SET    units_held = units_held + v_trade_units
        WHERE  investor_id = v_buy_investor_id AND asset_id = v_buy_asset_id;
    END IF;

    -- ── 3. Update seller's ownership ──────────────────────────
    SELECT COALESCE(units_held, 0)
    INTO   v_seller_prev_units
    FROM   OWNERSHIP
    WHERE  investor_id = v_sell_investor_id AND asset_id = v_sell_asset_id;

    -- Decrease seller's units (trigger logs history; DB constraint prevents < 0)
    UPDATE OWNERSHIP
    SET    units_held = units_held - v_trade_units
    WHERE  investor_id = v_sell_investor_id AND asset_id = v_sell_asset_id;

    -- Remove zero-unit ownership row if seller is fully out
    DELETE FROM OWNERSHIP
    WHERE  investor_id = v_sell_investor_id
      AND  asset_id    = v_sell_asset_id
      AND  units_held  = 0;

    -- ── 4. Wallet transactions ────────────────────────────────
    -- Deduct from buyer (BEFORE trigger verifies balance)
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (v_buy_investor_id, -v_trade_total, 'ASSET_PURCHASE', 'Trade Execution', NOW());

    -- Credit to seller
    INSERT INTO WALLET_TRANSACTION
        (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES
        (v_sell_investor_id, v_trade_total, 'ASSET_SALE', 'Trade Execution', NOW());

    COMMIT;

    SET p_trade_id = v_new_trade_id;
    SET p_success  = 1;
    SET p_message  = CONCAT('Trade #', v_new_trade_id, ' executed successfully: ',
                             v_trade_units, ' units of asset ', v_buy_asset_id,
                             ' at $', v_sell_price, '/unit. Total: $', v_trade_total);
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 5: sp_get_investor_summary
-- ------------------------------------------------------------
-- Returns a complete financial summary for one investor via
-- OUT parameters:
--   - investor name
--   - current wallet balance
--   - current portfolio market value
--   - number of distinct assets held
--   - total trade count (orders placed)
--
-- CALL sp_get_investor_summary(1, @nm, @bal, @pv, @cnt, @trades);
-- SELECT @nm, @bal, @pv, @cnt, @trades;
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_get_investor_summary;

DELIMITER $$
CREATE PROCEDURE sp_get_investor_summary(
    IN  p_investor_id        INT,
    OUT p_name               VARCHAR(100),
    OUT p_wallet_balance     DECIMAL(15, 2),
    OUT p_portfolio_value    DECIMAL(15, 2),
    OUT p_asset_count        INT,
    OUT p_trade_order_count  INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET p_name              = NULL;
        SET p_wallet_balance    = NULL;
        SET p_portfolio_value   = NULL;
        SET p_asset_count       = NULL;
        SET p_trade_order_count = NULL;
    END;

    -- Basic investor info
    SELECT name, wallet_balance
    INTO   p_name, p_wallet_balance
    FROM   INVESTOR
    WHERE  investor_id = p_investor_id;

    -- Portfolio value via function
    SET p_portfolio_value = fn_get_investor_portfolio_value(p_investor_id);

    -- Holdings count via function
    SET p_asset_count = fn_count_investor_holdings(p_investor_id);

    -- Trade orders placed
    SELECT COUNT(*)
    INTO   p_trade_order_count
    FROM   TRADE_ORDER
    WHERE  investor_id = p_investor_id;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 6: sp_verify_asset
-- ------------------------------------------------------------
-- Admin-level procedure to update an asset's verification
-- status and assign the verifying admin.
-- Valid statuses: 'Verified', 'Pending', 'Rejected'
--
-- CALL sp_verify_asset(5, 3, 'Verified', @ok, @msg);
-- SELECT @ok, @msg;
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_verify_asset;

DELIMITER $$
CREATE PROCEDURE sp_verify_asset(
    IN  p_asset_id   INT,
    IN  p_admin_id   INT,
    IN  p_status     VARCHAR(20),
    OUT p_success    TINYINT(1),
    OUT p_message    VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success = 0;
    END;

    -- Validate status value
    IF p_status NOT IN ('Verified', 'Pending', 'Rejected') THEN
        SET p_success = 0;
        SET p_message = CONCAT('Invalid status "', p_status,
                                '". Must be Verified, Pending, or Rejected.');
        LEAVE sp_verify_asset;
    END IF;

    -- Validate asset exists
    IF NOT EXISTS (SELECT 1 FROM ASSET WHERE asset_id = p_asset_id) THEN
        SET p_success = 0;
        SET p_message = CONCAT('Asset ID ', p_asset_id, ' does not exist.');
        LEAVE sp_verify_asset;
    END IF;

    -- Validate admin exists
    IF NOT EXISTS (SELECT 1 FROM ADMIN WHERE admin_id = p_admin_id) THEN
        SET p_success = 0;
        SET p_message = CONCAT('Admin ID ', p_admin_id, ' does not exist.');
        LEAVE sp_verify_asset;
    END IF;

    UPDATE ASSET
    SET    verification_status = p_status,
           verified_by         = p_admin_id
    WHERE  asset_id = p_asset_id;

    SET p_success = 1;
    SET p_message = CONCAT('Asset ', p_asset_id, ' status updated to "', p_status,
                            '" by admin ', p_admin_id, '.');
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 7: sp_place_buy_order
-- ------------------------------------------------------------
-- Creates a BUY order after validating that the investor has
-- sufficient wallet balance (units * price).
-- OUT p_order_id is set to the new order_id on success.
--
-- CALL sp_place_buy_order(2, 3, 10, 6500.00, @oid, @ok, @msg);
-- SELECT @oid, @ok, @msg;
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_place_buy_order;

DELIMITER $$
CREATE PROCEDURE sp_place_buy_order(
    IN  p_investor_id  INT,
    IN  p_asset_id     INT,
    IN  p_units        INT,
    IN  p_price        DECIMAL(10, 2),
    OUT p_order_id     INT,
    OUT p_success      TINYINT(1),
    OUT p_message      VARCHAR(255)
)
BEGIN
    DECLARE v_required DECIMAL(15, 2);
    DECLARE v_new_id   INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success  = 0;
        SET p_order_id = NULL;
        ROLLBACK;
    END;

    -- Validate inputs
    IF p_units <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Units must be greater than zero.';
        LEAVE sp_place_buy_order;
    END IF;

    IF p_price <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Price per unit must be greater than zero.';
        LEAVE sp_place_buy_order;
    END IF;

    SET v_required = p_units * p_price;

    IF fn_investor_has_sufficient_balance(p_investor_id, v_required) = 0 THEN
        SET p_success = 0;
        SET p_message = CONCAT('Insufficient wallet balance. Required: $', v_required,
                                ', Available: $', fn_get_wallet_balance(p_investor_id));
        LEAVE sp_place_buy_order;
    END IF;

    -- Next available order_id
    SELECT COALESCE(MAX(order_id), 0) + 1
    INTO   v_new_id
    FROM   TRADE_ORDER;

    START TRANSACTION;

    INSERT INTO TRADE_ORDER
        (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
    VALUES
        (v_new_id, p_investor_id, p_asset_id, 'BUY', p_price, p_units, CURDATE(), 'OPEN');

    COMMIT;

    SET p_order_id = v_new_id;
    SET p_success  = 1;
    SET p_message  = CONCAT('BUY order #', v_new_id, ' placed: ', p_units,
                             ' units of asset ', p_asset_id, ' at $', p_price, '/unit.');
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 8: sp_place_sell_order
-- ------------------------------------------------------------
-- Creates a SELL order after validating that the investor
-- actually holds enough units of the specified asset.
-- OUT p_order_id is set to the new order_id on success.
--
-- CALL sp_place_sell_order(1, 1, 5, 13500.00, @oid, @ok, @msg);
-- SELECT @oid, @ok, @msg;
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_place_sell_order;

DELIMITER $$
CREATE PROCEDURE sp_place_sell_order(
    IN  p_investor_id  INT,
    IN  p_asset_id     INT,
    IN  p_units        INT,
    IN  p_price        DECIMAL(10, 2),
    OUT p_order_id     INT,
    OUT p_success      TINYINT(1),
    OUT p_message      VARCHAR(255)
)
BEGIN
    DECLARE v_held   INT DEFAULT 0;
    DECLARE v_new_id INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success  = 0;
        SET p_order_id = NULL;
        ROLLBACK;
    END;

    IF p_units <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Units must be greater than zero.';
        LEAVE sp_place_sell_order;
    END IF;

    IF p_price <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Price per unit must be greater than zero.';
        LEAVE sp_place_sell_order;
    END IF;

    -- Check that investor holds enough units
    SELECT COALESCE(units_held, 0)
    INTO   v_held
    FROM   OWNERSHIP
    WHERE  investor_id = p_investor_id AND asset_id = p_asset_id;

    IF v_held < p_units THEN
        SET p_success = 0;
        SET p_message = CONCAT('Cannot sell ', p_units, ' units — investor ',
                                p_investor_id, ' only holds ', v_held,
                                ' units of asset ', p_asset_id, '.');
        LEAVE sp_place_sell_order;
    END IF;

    -- Next available order_id
    SELECT COALESCE(MAX(order_id), 0) + 1
    INTO   v_new_id
    FROM   TRADE_ORDER;

    START TRANSACTION;

    INSERT INTO TRADE_ORDER
        (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
    VALUES
        (v_new_id, p_investor_id, p_asset_id, 'SELL', p_price, p_units, CURDATE(), 'OPEN');

    COMMIT;

    SET p_order_id = v_new_id;
    SET p_success  = 1;
    SET p_message  = CONCAT('SELL order #', v_new_id, ' placed: ', p_units,
                             ' units of asset ', p_asset_id, ' at $', p_price, '/unit.');
END$$
DELIMITER ;


-- ============================================================
-- SECTION 4: QUICK VERIFICATION QUERIES
-- ============================================================
-- Run these after setup to confirm all objects were created.
-- ============================================================

-- List all functions and procedures just created:
SELECT ROUTINE_TYPE, ROUTINE_NAME
FROM   information_schema.ROUTINES
WHERE  ROUTINE_SCHEMA = 'fractional_ownership_db'
ORDER  BY ROUTINE_TYPE, ROUTINE_NAME;