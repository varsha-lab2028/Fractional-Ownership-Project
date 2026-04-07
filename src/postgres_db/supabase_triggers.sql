-- ================================================================
-- supabase_triggers.sql
-- Project  : Fractional Ownership Platform
-- Database : Supabase / PostgreSQL
--
-- FILE LOCATION: src/postgres_db/supabase_triggers.sql
--
-- IMPORTANT: This REPLACES the current supabase_triggers.sql
-- which incorrectly contains MySQL syntax (DELIMITER $$, etc.).
-- PostgreSQL triggers require a trigger FUNCTION + CREATE TRIGGER.
--
-- Run this in: Supabase Dashboard → SQL Editor → New Query
-- ================================================================


-- ================================================================
-- TRIGGER 1: Prevent negative wallet balance
-- Fires BEFORE INSERT on wallet_transaction.
-- Blocks any insert that would make wallet_balance go below 0.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_trg_prevent_negative_wallet_balance()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_current_balance NUMERIC(15,2);
BEGIN
    SELECT wallet_balance
    INTO   v_current_balance
    FROM   investor
    WHERE  investor_id = NEW.investor_id;

    IF (v_current_balance + NEW.amount) < 0 THEN
        RAISE EXCEPTION 'Insufficient wallet balance for this transaction. '
            'Current: %, Attempted change: %', v_current_balance, NEW.amount
            USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_negative_wallet_balance ON wallet_transaction;

CREATE TRIGGER trg_prevent_negative_wallet_balance
BEFORE INSERT ON wallet_transaction
FOR EACH ROW
EXECUTE FUNCTION fn_trg_prevent_negative_wallet_balance();


-- ================================================================
-- TRIGGER 2: Keep wallet_balance in sync
-- Fires AFTER INSERT on wallet_transaction.
-- Updates the denormalized wallet_balance column on INVESTOR.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_trg_update_wallet_balance()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE investor
    SET    wallet_balance = wallet_balance + NEW.amount
    WHERE  investor_id = NEW.investor_id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_update_wallet_balance_after_transaction ON wallet_transaction;

CREATE TRIGGER trg_update_wallet_balance_after_transaction
AFTER INSERT ON wallet_transaction
FOR EACH ROW
EXECUTE FUNCTION fn_trg_update_wallet_balance();


-- ================================================================
-- TRIGGER 3: Auto-mark trade orders as MATCHED after trade insert
-- Fires AFTER INSERT on trade.
-- Updates both buy_order and sell_order status to 'MATCHED'.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_trg_update_order_status_after_trade()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.buy_order_id IS NOT NULL THEN
        UPDATE trade_order
        SET    status = 'MATCHED'
        WHERE  order_id = NEW.buy_order_id;
    END IF;

    IF NEW.sell_order_id IS NOT NULL THEN
        UPDATE trade_order
        SET    status = 'MATCHED'
        WHERE  order_id = NEW.sell_order_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_update_order_status_after_trade ON trade;

CREATE TRIGGER trg_update_order_status_after_trade
AFTER INSERT ON trade
FOR EACH ROW
EXECUTE FUNCTION fn_trg_update_order_status_after_trade();


-- ================================================================
-- TRIGGER 4: Log ownership changes to ownership_history
-- Fires AFTER UPDATE on ownership when units_held changes.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_trg_log_ownership_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.units_held <> NEW.units_held THEN
        INSERT INTO ownership_history (
            investor_id, asset_id,
            units_before, units_after,
            change_date, change_type,
            trade_id, ipo_id
        )
        VALUES (
            NEW.investor_id, NEW.asset_id,
            OLD.units_held,  NEW.units_held,
            CURRENT_DATE,    'TRADE',
            NULL, NULL
        );
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_log_ownership_history_on_update ON ownership;

CREATE TRIGGER trg_log_ownership_history_on_update
AFTER UPDATE ON ownership
FOR EACH ROW
EXECUTE FUNCTION fn_trg_log_ownership_history();


-- ================================================================
-- TRIGGER 5: Prevent selling more units than owned
-- Fires BEFORE INSERT on trade_order when order_type = 'SELL'.
-- Blocks the insert if the investor doesn't hold enough units.
-- ================================================================

CREATE OR REPLACE FUNCTION fn_trg_validate_sell_order()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_held INT := 0;
BEGIN
    IF NEW.order_type = 'SELL' THEN
        SELECT COALESCE(units_held, 0) INTO v_held
        FROM   ownership
        WHERE  investor_id = NEW.investor_id
          AND  asset_id    = NEW.asset_id;

        IF v_held < NEW.units THEN
            RAISE EXCEPTION
                'Cannot place sell order: investor % only holds % units of asset %, '
                'but tried to sell %.', NEW.investor_id, v_held, NEW.asset_id, NEW.units
                USING ERRCODE = 'P0001';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validate_sell_order_units ON trade_order;

CREATE TRIGGER trg_validate_sell_order_units
BEFORE INSERT ON trade_order
FOR EACH ROW
EXECUTE FUNCTION fn_trg_validate_sell_order();


-- ================================================================
-- Verify triggers are installed
-- ================================================================
SELECT trigger_name, event_manipulation, event_object_table, action_timing
FROM   information_schema.triggers
WHERE  trigger_schema = 'public'
ORDER  BY event_object_table, trigger_name;