--name of the database


-- ============================================================
-- TRIGGER 1: trg_prevent_negative_wallet_balance
-- ============================================================
-- this will work on the table called WALLET_TRANSACTION
-- BEFORE INSERT type trigger
-- Purpose: Block any transaction that would push the investor's wallet_balance below zero.
DROP TRIGGER IF EXISTS trg_prevent_negative_wallet_balance;

DELIMITER $$ -- making '$$' the new full stop
CREATE TRIGGER trg_prevent_negative_wallet_balance
BEFORE INSERT ON WALLET_TRANSACTION
FOR EACH ROW
BEGIN
    DECLARE insufficient_funds CONDITION FOR SQLSTATE '45000';
    DECLARE EXIT HANDLER FOR insufficient_funds
    BEGIN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient wallet balance for this transaction';
    END;

    DECLARE current_balance DECIMAL(15, 2);
    -- Fetch the investor's current wallet_balance
    SELECT wallet_balance
    INTO current_balance
    FROM INVESTOR
    WHERE investor_id = NEW.investor_id;

    -- Check if the new transaction would cause a negative balance
    IF (current_balance + NEW.amount) < 0 THEN
        SIGNAL insufficient_funds;
    END IF;

END $$
DELIMITER;


-- ============================================================
-- TRIGGER 2: trg_update_wallet_balance_after_transaction
-- ============================================================
-- this will work on the table "WALLET_TRANSACTION"
-- AFTER TRIGGER type trigger
-- Purpose: Automatically update investor's wallet_balance
--          whenever a new transaction is successfully inserted.
--          Positive amount = credit. Negative amount = debit.
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
DELIMITER;


-- ============================================================
-- TRIGGER 3: trg_update_order_status_after_trade
-- ============================================================
-- this will work the table "TRADE"
-- AFTER INSERT type trigger
-- Purpose: When a trade is executed and inserted into the TRADE
--          table, automatically mark both the buy order and the
--          sell order as MATCHED in TRADE_ORDER.
DROP TRIGGER IF EXISTS trg_update_order_status_after_trade;

DELIMITER $$
CREATE TRIGGER trg_update_order_status_after_trade
AFTER INSERT ON TRADE
FOR EACH ROW
BEGIN
    -- Only update buy order if it exists and is not already MATCHED
    IF NEW.buy_order_id IS NOT NULL THEN
            UPDATE TRADE_ORDER
            SET status = 'MATCHED'
            WHERE order_id = NEW.buy_order_id;
    END IF;

    -- Only update sell order if it exists and is not already MATCHED
    IF NEW.sell_order_id IS NOT NULL THEN
       UPDATE TRADE_ORDER
       SET status = 'MATCHED'
       WHERE order_id = NEW.sell_order_id;
    END IF;

END$$
DELIMITER;


-- ============================================================
-- TRIGGER 4: trg_log_ownership_history_on_update
-- ============================================================
-- this will work on the table "OWNERSHIP"
-- AFTER UPDATE type trigger
-- Purpose: Every time units_held changes in OWNERSHIP (after a
--          trade settles), auto-log a record in OWNERSHIP_HISTORY
--          capturing units_before and units_after.
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
-- specifically added for Trigger 4, because when ownership get first record, it won't get logged
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


