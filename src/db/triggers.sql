USE fractional_ownership_db;

-- ============================================================
-- TRIGGER 1: trg_prevent_negative_wallet_balance
-- ============================================================
DROP TRIGGER IF EXISTS trg_prevent_negative_wallet_balance;

DELIMITER $$
CREATE TRIGGER trg_prevent_negative_wallet_balance
BEFORE INSERT ON WALLET_TRANSACTION
FOR EACH ROW
BEGIN
    DECLARE current_balance DECIMAL(15, 2);

    SELECT wallet_balance
    INTO current_balance
    FROM INVESTOR
    WHERE investor_id = NEW.investor_id;

    IF (current_balance + NEW.amount) < 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient wallet balance for this transaction';
    END IF;
END$$
DELIMITER ;


-- ============================================================
-- TRIGGER 2: trg_update_wallet_balance_after_transaction
-- ============================================================
DROP TRIGGER IF EXISTS trg_update_wallet_balance_after_transaction;

DELIMITER $$
CREATE TRIGGER trg_update_wallet_balance_after_transaction
AFTER INSERT ON WALLET_TRANSACTION
FOR EACH ROW
BEGIN
    UPDATE INVESTOR
    SET wallet_balance = wallet_balance + NEW.amount
    WHERE investor_id = NEW.investor_id;
END$$
DELIMITER ;


-- ============================================================
-- TRIGGER 3: trg_update_order_status_after_trade
-- ============================================================
DROP TRIGGER IF EXISTS trg_update_order_status_after_trade;

DELIMITER $$
CREATE TRIGGER trg_update_order_status_after_trade
AFTER INSERT ON TRADE
FOR EACH ROW
BEGIN
    IF NEW.buy_order_id IS NOT NULL THEN
        UPDATE TRADE_ORDER
        SET status = 'MATCHED'
        WHERE order_id = NEW.buy_order_id;
    END IF;

    IF NEW.sell_order_id IS NOT NULL THEN
        UPDATE TRADE_ORDER
        SET status = 'MATCHED'
        WHERE order_id = NEW.sell_order_id;
    END IF;
END$$
DELIMITER ;


-- ============================================================
-- TRIGGER 4: trg_log_ownership_history_on_update
-- ============================================================
DROP TRIGGER IF EXISTS trg_log_ownership_history_on_update;

DELIMITER $$
CREATE TRIGGER trg_log_ownership_history_on_update
AFTER UPDATE ON OWNERSHIP
FOR EACH ROW
BEGIN
    IF OLD.units_held <> NEW.units_held THEN
        INSERT INTO OWNERSHIP_HISTORY (
            investor_id,
            asset_id,
            units_before,
            units_after,
            change_date,
            change_type,
            trade_id,
            ipo_id
        )
        VALUES (
            NEW.investor_id,
            NEW.asset_id,
            OLD.units_held,
            NEW.units_held,
            CURDATE(),
            'HOLDING_UPDATE',
            NULL,
            NULL
        );
    END IF;
END$$
DELIMITER ;


-- ============================================================
-- TRIGGER 5: trg_log_ownership_history_on_insert
-- ============================================================
DROP TRIGGER IF EXISTS trg_log_ownership_history_on_insert;

DELIMITER $$
CREATE TRIGGER trg_log_ownership_history_on_insert
AFTER INSERT ON OWNERSHIP
FOR EACH ROW
BEGIN
    INSERT INTO OWNERSHIP_HISTORY (
        investor_id,
        asset_id,
        units_before,
        units_after,
        change_date,
        change_type,
        trade_id,
        ipo_id
    )
    VALUES (
        NEW.investor_id,
        NEW.asset_id,
        0,
        NEW.units_held,
        CURDATE(),
        'INITIAL_ALLOCATION',
        NULL,
        NULL
    );
END$$
DELIMITER ;


-- Confirm all 5 triggers installed:
SHOW TRIGGERS;