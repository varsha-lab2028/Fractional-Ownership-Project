-- ================================================================
-- supabase_functions_and_procedures.sql
-- Project  : Fractional Ownership Platform
-- Database : Supabase / PostgreSQL
--
-- FILE LOCATION: src/postgres_db/supabase_functions_and_procedures.sql
--
-- Run this in the Supabase SQL Editor (Dashboard → SQL Editor → New Query).
-- This is the PostgreSQL equivalent of src/db/functions_and_procedures.sql.
-- PostgreSQL uses CREATE OR REPLACE FUNCTION ... LANGUAGE plpgsql
-- instead of MySQL's DELIMITER $$ / CREATE FUNCTION ... END$$.
-- ================================================================


-- ================================================================
-- FUNCTION 1: fn_get_wallet_balance
-- Returns the current wallet balance for a given investor.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_get_wallet_balance(p_investor_id INT)
RETURNS NUMERIC(15,2)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_balance NUMERIC(15,2) := 0.00;
BEGIN
    SELECT wallet_balance
    INTO   v_balance
    FROM   investor
    WHERE  investor_id = p_investor_id;

    RETURN COALESCE(v_balance, 0.00);
END;
$$;


-- ================================================================
-- FUNCTION 2: fn_get_latest_valuation
-- Returns the most recent valuation amount for a given asset.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_get_latest_valuation(p_asset_id INT)
RETURNS NUMERIC(12,2)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_amount NUMERIC(12,2) := 0.00;
BEGIN
    SELECT valuation_amount
    INTO   v_amount
    FROM   valuation
    WHERE  asset_id       = p_asset_id
      AND  valuation_date = (
               SELECT MAX(valuation_date)
               FROM   valuation
               WHERE  asset_id = p_asset_id
           )
    LIMIT 1;

    RETURN COALESCE(v_amount, 0.00);
END;
$$;


-- ================================================================
-- FUNCTION 3: fn_get_total_units_held
-- Returns total units held across ALL investors for an asset.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_get_total_units_held(p_asset_id INT)
RETURNS INT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_total INT := 0;
BEGIN
    SELECT COALESCE(SUM(units_held), 0)
    INTO   v_total
    FROM   ownership
    WHERE  asset_id = p_asset_id;

    RETURN v_total;
END;
$$;


-- ================================================================
-- FUNCTION 4: fn_get_investor_portfolio_value
-- Returns the current market value of an investor's portfolio.
-- Each holding valued as: (units_held / total_units) * latest_valuation
-- ================================================================

CREATE OR REPLACE FUNCTION fn_get_investor_portfolio_value(p_investor_id INT)
RETURNS NUMERIC(15,2)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_value NUMERIC(15,2) := 0.00;
BEGIN
    SELECT COALESCE(
        SUM(
            (o.units_held::NUMERIC / i.total_units)
            * fn_get_latest_valuation(o.asset_id)
        ),
        0.00
    )
    INTO   v_value
    FROM   ownership o
    JOIN   ipo       i ON i.asset_id = o.asset_id
    WHERE  o.investor_id = p_investor_id;

    RETURN v_value;
END;
$$;


-- ================================================================
-- FUNCTION 5: fn_count_investor_holdings
-- Returns number of distinct assets an investor holds (units > 0).
-- ================================================================

CREATE OR REPLACE FUNCTION fn_count_investor_holdings(p_investor_id INT)
RETURNS INT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_count INT := 0;
BEGIN
    SELECT COUNT(*)
    INTO   v_count
    FROM   ownership
    WHERE  investor_id = p_investor_id
      AND  units_held  > 0;

    RETURN v_count;
END;
$$;


-- ================================================================
-- FUNCTION 6: fn_get_total_trade_volume
-- Returns total units traded for an asset across all completed trades.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_get_total_trade_volume(p_asset_id INT)
RETURNS INT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_volume INT := 0;
BEGIN
    SELECT COALESCE(SUM(t.trade_units), 0)
    INTO   v_volume
    FROM   trade       t
    JOIN   trade_order o ON o.order_id = t.buy_order_id
    WHERE  o.asset_id = p_asset_id;

    RETURN v_volume;
END;
$$;


-- ================================================================
-- FUNCTION 7: fn_investor_has_sufficient_balance
-- Returns TRUE (1) if investor has at least p_required_amount in wallet.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_investor_has_sufficient_balance(
    p_investor_id     INT,
    p_required_amount NUMERIC(15,2)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN fn_get_wallet_balance(p_investor_id) >= p_required_amount;
END;
$$;


-- ================================================================
-- PROCEDURE 1: sp_get_investor_holdings
-- Returns all assets held by an investor with current value.
-- In PostgreSQL, procedures that return result sets use RETURNS TABLE.
-- ================================================================

CREATE OR REPLACE FUNCTION sp_get_investor_holdings(p_investor_id INT)
RETURNS TABLE (
    investor_id          INT,
    asset_id             INT,
    asset_name           VARCHAR(100),
    category             VARCHAR(50),
    units_held           INT,
    ipo_price_per_unit   NUMERIC(10,2),
    latest_valuation     NUMERIC(12,2),
    current_holding_value NUMERIC(15,2)
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        o.investor_id,
        a.asset_id,
        a.asset_name,
        a.category,
        o.units_held,
        i.price_per_unit                                                AS ipo_price_per_unit,
        fn_get_latest_valuation(a.asset_id)                            AS latest_valuation,
        ROUND(
            (o.units_held::NUMERIC / i.total_units)
            * fn_get_latest_valuation(a.asset_id), 2
        )                                                              AS current_holding_value
    FROM  ownership o
    JOIN  asset     a ON a.asset_id = o.asset_id
    JOIN  ipo       i ON i.asset_id = o.asset_id
    WHERE o.investor_id = p_investor_id
      AND o.units_held  > 0
    ORDER BY current_holding_value DESC;
END;
$$;

-- Usage: SELECT * FROM sp_get_investor_holdings(1);


-- ================================================================
-- PROCEDURE 2: sp_deposit_to_wallet
-- Deposits an amount into an investor's wallet via WALLET_TRANSACTION.
-- ================================================================

CREATE OR REPLACE FUNCTION sp_deposit_to_wallet(
    p_investor_id INT,
    p_amount      NUMERIC(15,2),
    p_category    VARCHAR(100),
    OUT p_success BOOLEAN,
    OUT p_message TEXT
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_amount <= 0 THEN
        p_success := FALSE;
        p_message := 'Deposit amount must be greater than zero.';
        RETURN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM investor WHERE investor_id = p_investor_id) THEN
        p_success := FALSE;
        p_message := FORMAT('Investor ID %s does not exist.', p_investor_id);
        RETURN;
    END IF;

    INSERT INTO wallet_transaction (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (p_investor_id, p_amount, 'DEPOSIT', p_category, NOW());

    p_success := TRUE;
    p_message := FORMAT('Successfully deposited $%s to investor %s', p_amount, p_investor_id);

EXCEPTION WHEN OTHERS THEN
    p_success := FALSE;
    p_message := SQLERRM;
END;
$$;

-- Usage: SELECT * FROM sp_deposit_to_wallet(2, 1000.00, 'Bank Transfer');


-- ================================================================
-- PROCEDURE 3: sp_withdraw_from_wallet
-- Withdraws amount from investor wallet (checks balance first).
-- ================================================================

CREATE OR REPLACE FUNCTION sp_withdraw_from_wallet(
    p_investor_id INT,
    p_amount      NUMERIC(15,2),
    p_category    VARCHAR(100),
    OUT p_success BOOLEAN,
    OUT p_message TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_balance NUMERIC(15,2);
BEGIN
    IF p_amount <= 0 THEN
        p_success := FALSE;
        p_message := 'Withdrawal amount must be greater than zero.';
        RETURN;
    END IF;

    SELECT wallet_balance INTO v_balance FROM investor WHERE investor_id = p_investor_id;

    IF v_balance IS NULL THEN
        p_success := FALSE;
        p_message := FORMAT('Investor %s not found.', p_investor_id);
        RETURN;
    END IF;

    IF v_balance < p_amount THEN
        p_success := FALSE;
        p_message := FORMAT('Insufficient balance. Available: $%s, Required: $%s', v_balance, p_amount);
        RETURN;
    END IF;

    INSERT INTO wallet_transaction (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (p_investor_id, -p_amount, 'WITHDRAWAL', p_category, NOW());

    p_success := TRUE;
    p_message := FORMAT('Successfully withdrew $%s from investor %s', p_amount, p_investor_id);

EXCEPTION WHEN OTHERS THEN
    p_success := FALSE;
    p_message := SQLERRM;
END;
$$;


-- ================================================================
-- PROCEDURE 4: sp_execute_trade
-- Executes a matched buy/sell trade atomically with FOR UPDATE locks.
-- ================================================================

CREATE OR REPLACE FUNCTION sp_execute_trade(
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
BEGIN
    -- Lock both orders (prevents double-execution)
    SELECT investor_id, asset_id, units, status
    INTO   v_buy_investor, v_buy_asset, v_buy_units, v_buy_status
    FROM   trade_order
    WHERE  order_id = p_buy_order_id
    FOR UPDATE;

    SELECT investor_id, asset_id, units, price, status
    INTO   v_sell_investor, v_sell_asset, v_sell_units, v_sell_price, v_sell_status
    FROM   trade_order
    WHERE  order_id = p_sell_order_id
    FOR UPDATE;

    -- Validations
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
        p_message := FORMAT('Buy order #%s is not OPEN.', p_buy_order_id); RETURN;
    END IF;

    IF v_sell_status <> 'OPEN' THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := FORMAT('Sell order #%s is not OPEN.', p_sell_order_id); RETURN;
    END IF;

    IF v_buy_asset <> v_sell_asset THEN
        p_success := FALSE; p_trade_id := NULL;
        p_message := 'Orders are for different assets.'; RETURN;
    END IF;

    SET v_trade_units = LEAST(v_buy_units, v_sell_units);
    SET v_trade_total = v_trade_units * v_sell_price;

    SELECT COALESCE(MAX(trade_id), 0) + 1 INTO v_new_trade_id FROM trade;

    -- Insert trade (triggers will fire automatically)
    INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
    VALUES (v_new_trade_id, v_sell_price, v_trade_units, CURRENT_DATE, p_buy_order_id, p_sell_order_id);

    -- Update buyer ownership
    SELECT units_held INTO v_buyer_held
    FROM   ownership
    WHERE  investor_id = v_buy_investor AND asset_id = v_buy_asset
    FOR UPDATE;

    IF v_buyer_held IS NULL THEN
        INSERT INTO ownership (investor_id, asset_id, units_held)
        VALUES (v_buy_investor, v_buy_asset, v_trade_units);
    ELSE
        UPDATE ownership SET units_held = units_held + v_trade_units
        WHERE investor_id = v_buy_investor AND asset_id = v_buy_asset;
    END IF;

    -- Update seller ownership
    UPDATE ownership SET units_held = units_held - v_trade_units
    WHERE investor_id = v_sell_investor AND asset_id = v_sell_asset;

    DELETE FROM ownership
    WHERE investor_id = v_sell_investor AND asset_id = v_sell_asset AND units_held = 0;

    -- Wallet transactions
    INSERT INTO wallet_transaction (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (v_buy_investor,  -v_trade_total, 'ASSET_PURCHASE', 'Trade Execution', NOW());

    INSERT INTO wallet_transaction (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (v_sell_investor,  v_trade_total, 'ASSET_SALE',     'Trade Execution', NOW());

    -- Update order statuses (trigger may handle this too)
    UPDATE trade_order SET status = 'MATCHED' WHERE order_id = p_buy_order_id;
    UPDATE trade_order SET status = 'MATCHED' WHERE order_id = p_sell_order_id;

    p_trade_id := v_new_trade_id;
    p_success  := TRUE;
    p_message  := FORMAT('Trade #%s executed: %s units at $%s. Total: $%s',
                          v_new_trade_id, v_trade_units, v_sell_price, v_trade_total);

EXCEPTION WHEN OTHERS THEN
    p_success  := FALSE;
    p_trade_id := NULL;
    p_message  := SQLERRM;
    RAISE;  -- re-raise so caller's transaction rolls back
END;
$$;

-- Usage: SELECT * FROM sp_execute_trade(3, 13);


-- ================================================================
-- PROCEDURE 5: sp_verify_asset
-- Admin approves or rejects an asset's verification status.
-- ================================================================

CREATE OR REPLACE FUNCTION sp_verify_asset(
    p_asset_id  INT,
    p_admin_id  INT,
    p_status    VARCHAR(20),
    OUT p_success BOOLEAN,
    OUT p_message TEXT
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_status NOT IN ('Verified', 'Pending', 'Rejected') THEN
        p_success := FALSE;
        p_message := FORMAT('Invalid status: %s', p_status);
        RETURN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM asset WHERE asset_id = p_asset_id) THEN
        p_success := FALSE;
        p_message := FORMAT('Asset ID %s does not exist.', p_asset_id);
        RETURN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM admin WHERE admin_id = p_admin_id) THEN
        p_success := FALSE;
        p_message := FORMAT('Admin ID %s does not exist.', p_admin_id);
        RETURN;
    END IF;

    UPDATE asset
    SET    verification_status = p_status,
           verified_by         = p_admin_id
    WHERE  asset_id = p_asset_id;

    p_success := TRUE;
    p_message := FORMAT('Asset %s set to %s by admin %s', p_asset_id, p_status, p_admin_id);

EXCEPTION WHEN OTHERS THEN
    p_success := FALSE;
    p_message := SQLERRM;
END;
$$;


-- ================================================================
-- PROCEDURE 6: sp_subscribe_to_ipo
-- Investor buys units in an active IPO (fully transactional).
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
    v_asset_id    INT;
    v_price       NUMERIC(10,2);
    v_total       NUMERIC(15,2);
    v_balance     NUMERIC(15,2);
    v_ipo_start   DATE;
    v_ipo_end     DATE;
    v_total_units INT;
    v_units_sold  INT;
    v_units_before INT := 0;
BEGIN
    IF p_units <= 0 THEN
        p_success := FALSE;
        p_message := 'Units must be greater than 0.';
        RETURN;
    END IF;

    -- Lock IPO row
    SELECT asset_id, price_per_unit, ipo_start_date, ipo_end_date, total_units, units_sold
    INTO   v_asset_id, v_price, v_ipo_start, v_ipo_end, v_total_units, v_units_sold
    FROM   ipo
    WHERE  ipo_id = p_ipo_id
    FOR UPDATE;

    IF v_asset_id IS NULL THEN
        p_success := FALSE;
        p_message := FORMAT('IPO #%s does not exist.', p_ipo_id); RETURN;
    END IF;

    IF CURRENT_DATE < v_ipo_start OR CURRENT_DATE > v_ipo_end THEN
        p_success := FALSE;
        p_message := FORMAT('IPO #%s is not currently active.', p_ipo_id); RETURN;
    END IF;

    IF v_units_sold + p_units > v_total_units THEN
        p_success := FALSE;
        p_message := FORMAT('Not enough units left in IPO #%s.', p_ipo_id); RETURN;
    END IF;

    -- Lock investor wallet
    SELECT wallet_balance INTO v_balance
    FROM   investor
    WHERE  investor_id = p_investor_id
    FOR UPDATE;

    IF v_balance IS NULL THEN
        p_success := FALSE;
        p_message := FORMAT('Investor #%s does not exist.', p_investor_id); RETURN;
    END IF;

    SET v_total = p_units * v_price;

    IF v_balance < v_total THEN
        p_success := FALSE;
        p_message := FORMAT('Insufficient balance. Required: %s, Available: %s', v_total, v_balance); RETURN;
    END IF;

    -- Lock existing ownership
    SELECT COALESCE(units_held, 0) INTO v_units_before
    FROM   ownership
    WHERE  investor_id = p_investor_id AND asset_id = v_asset_id
    FOR UPDATE;

    -- Perform writes
    UPDATE investor SET wallet_balance = wallet_balance - v_total WHERE investor_id = p_investor_id;
    UPDATE ipo SET units_sold = units_sold + p_units WHERE ipo_id = p_ipo_id;

    INSERT INTO ownership (investor_id, asset_id, units_held)
    VALUES (p_investor_id, v_asset_id, p_units)
    ON CONFLICT (investor_id, asset_id) DO UPDATE SET units_held = ownership.units_held + EXCLUDED.units_held;

    INSERT INTO wallet_transaction (investor_id, amount, transaction_type, transfer_category, transaction_date)
    VALUES (p_investor_id, -v_total, 'ASSET_PURCHASE', 'IPO Subscription', NOW());

    INSERT INTO ownership_history (investor_id, asset_id, units_before, units_after, change_date, change_type, trade_id, ipo_id)
    VALUES (p_investor_id, v_asset_id, v_units_before, v_units_before + p_units, CURRENT_DATE, 'IPO', NULL, p_ipo_id);

    p_success := TRUE;
    p_message := FORMAT('IPO subscription successful. Investor %s bought %s unit(s) in IPO #%s.',
                         p_investor_id, p_units, p_ipo_id);

EXCEPTION WHEN OTHERS THEN
    p_success := FALSE;
    p_message := SQLERRM;
    RAISE;
END;
$$;

-- Usage: SELECT * FROM sp_subscribe_to_ipo(2, 2, 5);


-- ================================================================
-- PROCEDURE 7: sp_get_investor_summary
-- Returns summary stats for an investor (portfolio value, assets, etc.)
-- ================================================================

CREATE OR REPLACE FUNCTION sp_get_investor_summary(p_investor_id INT)
RETURNS TABLE (
    investor_name      VARCHAR(100),
    wallet_balance     NUMERIC(15,2),
    portfolio_value    NUMERIC(15,2),
    asset_count        INT,
    trade_order_count  BIGINT
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        i.investor_name,
        i.wallet_balance,
        fn_get_investor_portfolio_value(p_investor_id),
        fn_count_investor_holdings(p_investor_id),
        (SELECT COUNT(*) FROM trade_order WHERE investor_id = p_investor_id)
    FROM investor i
    WHERE i.investor_id = p_investor_id;
END;
$$;

-- Usage: SELECT * FROM sp_get_investor_summary(1);


-- ================================================================
-- CONFIRM: list all functions installed
-- ================================================================
SELECT routine_name, routine_type
FROM   information_schema.routines
WHERE  routine_schema = 'public'
  AND  routine_type   = 'FUNCTION'
ORDER  BY routine_name;