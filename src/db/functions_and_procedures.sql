USE fractional_ownership_db;

-- ------------------------------------------------------------
-- FUNCTION 1: fn_get_wallet_balance
-- ------------------------------------------------------------
-- Returns the current wallet balance for a given investor.

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

-- ------------------------------------------------------------
-- PROCEDURE 2: sp_deposit_to_wallet
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
    END;

    IF p_amount <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Deposit amount must be greater than zero.';
    ELSEIF NOT EXISTS (SELECT 1 FROM INVESTOR WHERE investor_id = p_investor_id) THEN
        SET p_success = 0;
        SET p_message = CONCAT('Investor ID ', p_investor_id, ' does not exist.');
    ELSE
        INSERT INTO WALLET_TRANSACTION
            (investor_id, amount, transaction_type, transfer_category, transaction_date)
        VALUES
            (p_investor_id, p_amount, 'DEPOSIT', p_category, NOW());
        SET p_success = 1;
        SET p_message = CONCAT('Successfully deposited $', p_amount, ' to investor ', p_investor_id);
    END IF;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 3: sp_withdraw_from_wallet
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
    END;

    IF p_amount <= 0 THEN
        SET p_success = 0;
        SET p_message = 'Withdrawal amount must be greater than zero.';
    ELSE
        SELECT wallet_balance INTO v_balance
        FROM INVESTOR WHERE investor_id = p_investor_id;

        IF v_balance < p_amount THEN
            SET p_success = 0;
            SET p_message = CONCAT('Insufficient balance. Available: $', v_balance, ', Required: $', p_amount);
        ELSE
            INSERT INTO WALLET_TRANSACTION
                (investor_id, amount, transaction_type, transfer_category, transaction_date)
            VALUES
                (p_investor_id, -p_amount, 'WITHDRAWAL', p_category, NOW());
            SET p_success = 1;
            SET p_message = CONCAT('Successfully withdrew $', p_amount, ' from investor ', p_investor_id);
        END IF;
    END IF;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 4: sp_execute_trade
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
    DECLARE v_buyer_prev_units INT DEFAULT 0;
    DECLARE v_new_trade_id     INT;
    DECLARE v_valid            TINYINT DEFAULT 1;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success = 0;
        SET p_trade_id = NULL;
        ROLLBACK;
    END;

    -- Load buy order
    SELECT investor_id, asset_id, units, status
    INTO v_buy_investor_id, v_buy_asset_id, v_buy_units, v_buy_status
    FROM TRADE_ORDER WHERE order_id = p_buy_order_id;

    -- Load sell order
    SELECT investor_id, asset_id, units, price, status
    INTO v_sell_investor_id, v_sell_asset_id, v_sell_units, v_sell_price, v_sell_status
    FROM TRADE_ORDER WHERE order_id = p_sell_order_id;

    -- Validate
    IF v_buy_investor_id IS NULL THEN
        SET v_valid = 0;
        SET p_message = CONCAT('Buy order #', p_buy_order_id, ' not found.');
    ELSEIF v_sell_investor_id IS NULL THEN
        SET v_valid = 0;
        SET p_message = CONCAT('Sell order #', p_sell_order_id, ' not found.');
    ELSEIF v_buy_status <> 'OPEN' THEN
        SET v_valid = 0;
        SET p_message = CONCAT('Buy order #', p_buy_order_id, ' is not OPEN.');
    ELSEIF v_sell_status <> 'OPEN' THEN
        SET v_valid = 0;
        SET p_message = CONCAT('Sell order #', p_sell_order_id, ' is not OPEN.');
    ELSEIF v_buy_asset_id <> v_sell_asset_id THEN
        SET v_valid = 0;
        SET p_message = 'Orders are for different assets.';
    END IF;

    IF v_valid = 1 THEN
        SET v_trade_units = LEAST(v_buy_units, v_sell_units);
        SET v_trade_total = v_trade_units * v_sell_price;

        SELECT COALESCE(MAX(trade_id), 0) + 1 INTO v_new_trade_id FROM TRADE;

        START TRANSACTION;

        INSERT INTO TRADE (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
        VALUES (v_new_trade_id, v_sell_price, v_trade_units, CURDATE(), p_buy_order_id, p_sell_order_id);

        -- Update buyer ownership
        IF EXISTS (SELECT 1 FROM OWNERSHIP WHERE investor_id = v_buy_investor_id AND asset_id = v_buy_asset_id) THEN
            UPDATE OWNERSHIP SET units_held = units_held + v_trade_units
            WHERE investor_id = v_buy_investor_id AND asset_id = v_buy_asset_id;
        ELSE
            INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
            VALUES (v_buy_investor_id, v_buy_asset_id, v_trade_units);
        END IF;

        -- Update seller ownership
        UPDATE OWNERSHIP SET units_held = units_held - v_trade_units
        WHERE investor_id = v_sell_investor_id AND asset_id = v_sell_asset_id;

        DELETE FROM OWNERSHIP
        WHERE investor_id = v_sell_investor_id AND asset_id = v_sell_asset_id AND units_held = 0;

        -- Wallet transactions
        INSERT INTO WALLET_TRANSACTION (investor_id, amount, transaction_type, transfer_category, transaction_date)
        VALUES (v_buy_investor_id, -v_trade_total, 'ASSET_PURCHASE', 'Trade Execution', NOW());

        INSERT INTO WALLET_TRANSACTION (investor_id, amount, transaction_type, transfer_category, transaction_date)
        VALUES (v_sell_investor_id, v_trade_total, 'ASSET_SALE', 'Trade Execution', NOW());

        COMMIT;

        SET p_trade_id = v_new_trade_id;
        SET p_success  = 1;
        SET p_message  = CONCAT('Trade #', v_new_trade_id, ' executed: ', v_trade_units,
                                 ' units at $', v_sell_price, '. Total: $', v_trade_total);
    ELSE
        SET p_success  = 0;
        SET p_trade_id = NULL;
    END IF;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 5: sp_get_investor_summary (already installed, recreating cleanly)
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
    SELECT name, wallet_balance INTO p_name, p_wallet_balance
    FROM INVESTOR WHERE investor_id = p_investor_id;

    SET p_portfolio_value = fn_get_investor_portfolio_value(p_investor_id);
    SET p_asset_count     = fn_count_investor_holdings(p_investor_id);

    SELECT COUNT(*) INTO p_trade_order_count
    FROM TRADE_ORDER WHERE investor_id = p_investor_id;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 6: sp_verify_asset
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

    IF p_status NOT IN ('Verified', 'Pending', 'Rejected') THEN
        SET p_success = 0;
        SET p_message = CONCAT('Invalid status: ', p_status);
    ELSEIF NOT EXISTS (SELECT 1 FROM ASSET WHERE asset_id = p_asset_id) THEN
        SET p_success = 0;
        SET p_message = CONCAT('Asset ID ', p_asset_id, ' does not exist.');
    ELSEIF NOT EXISTS (SELECT 1 FROM ADMIN WHERE admin_id = p_admin_id) THEN
        SET p_success = 0;
        SET p_message = CONCAT('Admin ID ', p_admin_id, ' does not exist.');
    ELSE
        UPDATE ASSET SET verification_status = p_status, verified_by = p_admin_id
        WHERE asset_id = p_asset_id;
        SET p_success = 1;
        SET p_message = CONCAT('Asset ', p_asset_id, ' set to ', p_status, ' by admin ', p_admin_id);
    END IF;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 7: sp_place_buy_order
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
    DECLARE v_balance  DECIMAL(15, 2);
    DECLARE v_new_id   INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 p_message = MESSAGE_TEXT;
        SET p_success  = 0;
        SET p_order_id = NULL;
    END;

    SET v_required = p_units * p_price;
    SELECT wallet_balance INTO v_balance FROM INVESTOR WHERE investor_id = p_investor_id;

    IF p_units <= 0 THEN
        SET p_success = 0;
        SET p_order_id = NULL;
        SET p_message = 'Units must be greater than zero.';
    ELSEIF p_price <= 0 THEN
        SET p_success = 0;
        SET p_order_id = NULL;
        SET p_message = 'Price must be greater than zero.';
    ELSEIF v_balance < v_required THEN
        SET p_success = 0;
        SET p_order_id = NULL;
        SET p_message = CONCAT('Insufficient balance. Required: $', v_required, ', Available: $', v_balance);
    ELSE
        SELECT COALESCE(MAX(order_id), 0) + 1 INTO v_new_id FROM TRADE_ORDER;

        INSERT INTO TRADE_ORDER (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
        VALUES (v_new_id, p_investor_id, p_asset_id, 'BUY', p_price, p_units, CURDATE(), 'OPEN');

        SET p_order_id = v_new_id;
        SET p_success  = 1;
        SET p_message  = CONCAT('BUY order #', v_new_id, ' placed: ', p_units, ' units at $', p_price);
    END IF;
END$$
DELIMITER ;


-- ------------------------------------------------------------
-- PROCEDURE 8: sp_place_sell_order
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
    END;

    SELECT COALESCE(units_held, 0) INTO v_held
    FROM OWNERSHIP WHERE investor_id = p_investor_id AND asset_id = p_asset_id;

    IF p_units <= 0 THEN
        SET p_success = 0;
        SET p_order_id = NULL;
        SET p_message = 'Units must be greater than zero.';
    ELSEIF p_price <= 0 THEN
        SET p_success = 0;
        SET p_order_id = NULL;
        SET p_message = 'Price must be greater than zero.';
    ELSEIF v_held < p_units THEN
        SET p_success = 0;
        SET p_order_id = NULL;
        SET p_message = CONCAT('Cannot sell ', p_units, ' units — only holds ', v_held, ' units.');
    ELSE
        SELECT COALESCE(MAX(order_id), 0) + 1 INTO v_new_id FROM TRADE_ORDER;

        INSERT INTO TRADE_ORDER (order_id, investor_id, asset_id, order_type, price, units, order_date, status)
        VALUES (v_new_id, p_investor_id, p_asset_id, 'SELL', p_price, p_units, CURDATE(), 'OPEN');

        SET p_order_id = v_new_id;
        SET p_success  = 1;
        SET p_message  = CONCAT('SELL order #', v_new_id, ' placed: ', p_units, ' units at $', p_price);
    END IF;
END$$
DELIMITER ;


-- Confirm all procedures installed:
SELECT ROUTINE_NAME, ROUTINE_TYPE
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'fractional_ownership_db'
ORDER BY ROUTINE_TYPE, ROUTINE_NAME;