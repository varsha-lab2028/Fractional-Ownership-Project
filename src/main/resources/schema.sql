-- ============================================================
-- FRACTIONAL OWNERSHIP MARKETPLACE - Full Schema + Seed Data
-- ============================================================

DROP DATABASE IF EXISTS fractional_ownership_db;
CREATE DATABASE fractional_ownership_db;
USE fractional_ownership_db;

-- ============================================================
-- TABLES
-- ============================================================

CREATE TABLE ADMIN (
    admin_id    INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,  -- bcrypt hash
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE INVESTOR (
    investor_id   INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) UNIQUE NOT NULL,
    password      VARCHAR(255) NOT NULL,  -- bcrypt hash
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(150) UNIQUE NOT NULL,
    phone         VARCHAR(20),
    wallet_balance DECIMAL(15,2) DEFAULT 100000.00,
    kyc_verified  BOOLEAN DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ASSET (
    asset_id          INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(200) NOT NULL,
    category          VARCHAR(100) NOT NULL,
    description       TEXT,
    total_units       INT NOT NULL,
    available_units   INT NOT NULL,
    current_price     DECIMAL(15,2) NOT NULL,
    total_value       DECIMAL(15,2) NOT NULL,
    verification_status ENUM('PENDING','VERIFIED','REJECTED') DEFAULT 'PENDING',
    listed_by_admin   INT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listed_by_admin) REFERENCES ADMIN(admin_id)
);

CREATE TABLE IPO (
    ipo_id          INT AUTO_INCREMENT PRIMARY KEY,
    asset_id        INT NOT NULL,
    ipo_price       DECIMAL(15,2) NOT NULL,
    total_units     INT NOT NULL,
    units_sold      INT DEFAULT 0,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          ENUM('UPCOMING','OPEN','CLOSED') DEFAULT 'UPCOMING',
    launched_by     INT,
    FOREIGN KEY (asset_id) REFERENCES ASSET(asset_id),
    FOREIGN KEY (launched_by) REFERENCES ADMIN(admin_id)
);

CREATE TABLE OWNERSHIP (
    ownership_id   INT AUTO_INCREMENT PRIMARY KEY,
    investor_id    INT NOT NULL,
    asset_id       INT NOT NULL,
    units_owned    INT NOT NULL,
    avg_buy_price  DECIMAL(15,2) NOT NULL,
    acquired_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_investor_asset (investor_id, asset_id),
    FOREIGN KEY (investor_id) REFERENCES INVESTOR(investor_id),
    FOREIGN KEY (asset_id) REFERENCES ASSET(asset_id)
);

CREATE TABLE TRADE_ORDER (
    order_id       INT AUTO_INCREMENT PRIMARY KEY,
    investor_id    INT NOT NULL,
    asset_id       INT NOT NULL,
    order_type     ENUM('BUY','SELL') NOT NULL,
    units          INT NOT NULL,
    price_per_unit DECIMAL(15,2) NOT NULL,
    status         ENUM('PENDING','EXECUTED','CANCELLED') DEFAULT 'PENDING',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (investor_id) REFERENCES INVESTOR(investor_id),
    FOREIGN KEY (asset_id) REFERENCES ASSET(asset_id)
);

CREATE TABLE TRADE (
    trade_id       INT AUTO_INCREMENT PRIMARY KEY,
    buy_order_id   INT,
    sell_order_id  INT,
    asset_id       INT NOT NULL,
    buyer_id       INT NOT NULL,
    seller_id      INT,
    units          INT NOT NULL,
    price_per_unit DECIMAL(15,2) NOT NULL,
    total_amount   DECIMAL(15,2) NOT NULL,
    traded_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id) REFERENCES ASSET(asset_id),
    FOREIGN KEY (buyer_id) REFERENCES INVESTOR(investor_id),
    FOREIGN KEY (seller_id) REFERENCES INVESTOR(investor_id)
);

CREATE TABLE OWNERSHIP_HISTORY (
    history_id     INT AUTO_INCREMENT PRIMARY KEY,
    investor_id    INT NOT NULL,
    asset_id       INT NOT NULL,
    units_change   INT NOT NULL,
    change_type    ENUM('BUY','SELL','IPO') NOT NULL,
    price_per_unit DECIMAL(15,2) NOT NULL,
    changed_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (investor_id) REFERENCES INVESTOR(investor_id),
    FOREIGN KEY (asset_id) REFERENCES ASSET(asset_id)
);

CREATE TABLE VALUATION (
    valuation_id   INT AUTO_INCREMENT PRIMARY KEY,
    asset_id       INT NOT NULL,
    valuation_date DATE NOT NULL,
    price_per_unit DECIMAL(15,2) NOT NULL,
    total_value    DECIMAL(15,2) NOT NULL,
    recorded_by    INT,
    FOREIGN KEY (asset_id) REFERENCES ASSET(asset_id),
    FOREIGN KEY (recorded_by) REFERENCES ADMIN(admin_id)
);

-- ============================================================
-- STORED PROCEDURES
-- ============================================================

DELIMITER //

-- Procedure: Buy IPO Units
CREATE PROCEDURE sp_buy_ipo(
    IN p_investor_id INT,
    IN p_ipo_id INT,
    IN p_units INT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
BEGIN
    DECLARE v_asset_id INT;
    DECLARE v_ipo_price DECIMAL(15,2);
    DECLARE v_units_remaining INT;
    DECLARE v_wallet DECIMAL(15,2);
    DECLARE v_total_cost DECIMAL(15,2);
    DECLARE v_existing_units INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = 'Transaction failed due to an error';
    END;

    START TRANSACTION;

    SELECT i.asset_id, i.ipo_price, (i.total_units - i.units_sold)
    INTO v_asset_id, v_ipo_price, v_units_remaining
    FROM IPO i WHERE i.ipo_id = p_ipo_id AND i.status = 'OPEN'
    FOR UPDATE;

    IF v_asset_id IS NULL THEN
        SET p_success = FALSE; SET p_message = 'IPO is not open'; ROLLBACK;
    ELSEIF v_units_remaining < p_units THEN
        SET p_success = FALSE; SET p_message = 'Not enough units available'; ROLLBACK;
    ELSE
        SET v_total_cost = p_units * v_ipo_price;
        SELECT wallet_balance INTO v_wallet FROM INVESTOR WHERE investor_id = p_investor_id FOR UPDATE;
        IF v_wallet < v_total_cost THEN
            SET p_success = FALSE; SET p_message = 'Insufficient wallet balance'; ROLLBACK;
        ELSE
            UPDATE INVESTOR SET wallet_balance = wallet_balance - v_total_cost WHERE investor_id = p_investor_id;
            UPDATE IPO SET units_sold = units_sold + p_units WHERE ipo_id = p_ipo_id;
            UPDATE ASSET SET available_units = available_units - p_units WHERE asset_id = v_asset_id;

            SELECT units_owned INTO v_existing_units FROM OWNERSHIP
            WHERE investor_id = p_investor_id AND asset_id = v_asset_id;

            IF v_existing_units > 0 THEN
                UPDATE OWNERSHIP
                SET units_owned = units_owned + p_units,
                    avg_buy_price = ((avg_buy_price * units_owned) + (v_ipo_price * p_units)) / (units_owned + p_units)
                WHERE investor_id = p_investor_id AND asset_id = v_asset_id;
            ELSE
                INSERT INTO OWNERSHIP(investor_id, asset_id, units_owned, avg_buy_price)
                VALUES(p_investor_id, v_asset_id, p_units, v_ipo_price);
            END IF;

            INSERT INTO OWNERSHIP_HISTORY(investor_id, asset_id, units_change, change_type, price_per_unit)
            VALUES(p_investor_id, v_asset_id, p_units, 'IPO', v_ipo_price);

            INSERT INTO TRADE(asset_id, buyer_id, seller_id, units, price_per_unit, total_amount)
            VALUES(v_asset_id, p_investor_id, NULL, p_units, v_ipo_price, v_total_cost);

            COMMIT;
            SET p_success = TRUE;
            SET p_message = CONCAT('Successfully purchased ', p_units, ' units');
        END IF;
    END IF;
END //

-- Procedure: Place Trade Order
CREATE PROCEDURE sp_place_order(
    IN p_investor_id INT,
    IN p_asset_id INT,
    IN p_order_type VARCHAR(4),
    IN p_units INT,
    IN p_price DECIMAL(15,2),
    OUT p_order_id INT,
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
BEGIN
    DECLARE v_wallet DECIMAL(15,2);
    DECLARE v_owned_units INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_success = FALSE;
        SET p_message = 'Order placement failed';
    END;

    START TRANSACTION;

    IF p_order_type = 'BUY' THEN
        SELECT wallet_balance INTO v_wallet FROM INVESTOR WHERE investor_id = p_investor_id FOR UPDATE;
        IF v_wallet < p_price * p_units THEN
            SET p_success = FALSE; SET p_message = 'Insufficient balance'; ROLLBACK;
        ELSE
            INSERT INTO TRADE_ORDER(investor_id, asset_id, order_type, units, price_per_unit, status)
            VALUES(p_investor_id, p_asset_id, 'BUY', p_units, p_price, 'PENDING');
            SET p_order_id = LAST_INSERT_ID();
            COMMIT;
            SET p_success = TRUE; SET p_message = 'Buy order placed';
        END IF;
    ELSE
        SELECT units_owned INTO v_owned_units FROM OWNERSHIP
        WHERE investor_id = p_investor_id AND asset_id = p_asset_id FOR UPDATE;
        IF v_owned_units < p_units THEN
            SET p_success = FALSE; SET p_message = 'Not enough units to sell'; ROLLBACK;
        ELSE
            INSERT INTO TRADE_ORDER(investor_id, asset_id, order_type, units, price_per_unit, status)
            VALUES(p_investor_id, p_asset_id, 'SELL', p_units, p_price, 'PENDING');
            SET p_order_id = LAST_INSERT_ID();
            COMMIT;
            SET p_success = TRUE; SET p_message = 'Sell order placed';
        END IF;
    END IF;
END //

-- Procedure: Execute Matching Orders
CREATE PROCEDURE sp_execute_matching_orders(IN p_asset_id INT)
BEGIN
    DECLARE v_buy_order_id INT;
    DECLARE v_sell_order_id INT;
    DECLARE v_buyer_id INT;
    DECLARE v_seller_id INT;
    DECLARE v_buy_units INT;
    DECLARE v_sell_units INT;
    DECLARE v_trade_units INT;
    DECLARE v_price DECIMAL(15,2);
    DECLARE done INT DEFAULT FALSE;

    DECLARE buy_cursor CURSOR FOR
        SELECT order_id, investor_id, units, price_per_unit FROM TRADE_ORDER
        WHERE asset_id = p_asset_id AND order_type = 'BUY' AND status = 'PENDING'
        ORDER BY price_per_unit DESC, created_at ASC;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN buy_cursor;

    read_loop: LOOP
        FETCH buy_cursor INTO v_buy_order_id, v_buyer_id, v_buy_units, v_price;
        IF done THEN LEAVE read_loop; END IF;

        -- Match with a sell order at or below buy price
        SELECT order_id, investor_id, units INTO v_sell_order_id, v_seller_id, v_sell_units
        FROM TRADE_ORDER
        WHERE asset_id = p_asset_id AND order_type = 'SELL' AND status = 'PENDING'
          AND price_per_unit <= v_price
        ORDER BY price_per_unit ASC, created_at ASC LIMIT 1;

        IF v_sell_order_id IS NOT NULL THEN
            SET v_trade_units = LEAST(v_buy_units, v_sell_units);

            -- Record trade
            INSERT INTO TRADE(buy_order_id, sell_order_id, asset_id, buyer_id, seller_id, units, price_per_unit, total_amount)
            VALUES(v_buy_order_id, v_sell_order_id, p_asset_id, v_buyer_id, v_seller_id, v_trade_units, v_price, v_trade_units * v_price);

            -- Update buyer wallet and ownership
            UPDATE INVESTOR SET wallet_balance = wallet_balance - (v_trade_units * v_price) WHERE investor_id = v_buyer_id;
            UPDATE INVESTOR SET wallet_balance = wallet_balance + (v_trade_units * v_price) WHERE investor_id = v_seller_id;

            -- Update buyer ownership
            INSERT INTO OWNERSHIP(investor_id, asset_id, units_owned, avg_buy_price)
            VALUES(v_buyer_id, p_asset_id, v_trade_units, v_price)
            ON DUPLICATE KEY UPDATE
                avg_buy_price = ((avg_buy_price * units_owned) + (v_price * v_trade_units)) / (units_owned + v_trade_units),
                units_owned = units_owned + v_trade_units;

            -- Update seller ownership
            UPDATE OWNERSHIP SET units_owned = units_owned - v_trade_units
            WHERE investor_id = v_seller_id AND asset_id = p_asset_id;
            DELETE FROM OWNERSHIP WHERE investor_id = v_seller_id AND asset_id = p_asset_id AND units_owned = 0;

            -- History
            INSERT INTO OWNERSHIP_HISTORY(investor_id, asset_id, units_change, change_type, price_per_unit)
            VALUES(v_buyer_id, p_asset_id, v_trade_units, 'BUY', v_price);
            INSERT INTO OWNERSHIP_HISTORY(investor_id, asset_id, units_change, change_type, price_per_unit)
            VALUES(v_seller_id, p_asset_id, -v_trade_units, 'SELL', v_price);

            -- Update orders
            IF v_buy_units = v_trade_units THEN
                UPDATE TRADE_ORDER SET status = 'EXECUTED' WHERE order_id = v_buy_order_id;
            ELSE
                UPDATE TRADE_ORDER SET units = units - v_trade_units WHERE order_id = v_buy_order_id;
            END IF;
            IF v_sell_units = v_trade_units THEN
                UPDATE TRADE_ORDER SET status = 'EXECUTED' WHERE order_id = v_sell_order_id;
            ELSE
                UPDATE TRADE_ORDER SET units = units - v_trade_units WHERE order_id = v_sell_order_id;
            END IF;
        END IF;
    END LOOP;
    CLOSE buy_cursor;
END //

-- Procedure: Add Wallet Funds
CREATE PROCEDURE sp_add_wallet(IN p_investor_id INT, IN p_amount DECIMAL(15,2), OUT p_new_balance DECIMAL(15,2))
BEGIN
    UPDATE INVESTOR SET wallet_balance = wallet_balance + p_amount WHERE investor_id = p_investor_id;
    SELECT wallet_balance INTO p_new_balance FROM INVESTOR WHERE investor_id = p_investor_id;
END //

-- Procedure: Verify Asset
CREATE PROCEDURE sp_verify_asset(IN p_asset_id INT, IN p_admin_id INT, IN p_status VARCHAR(20))
BEGIN
    UPDATE ASSET SET verification_status = p_status WHERE asset_id = p_asset_id;
END //

-- Procedure: Launch IPO
CREATE PROCEDURE sp_launch_ipo(
    IN p_asset_id INT,
    IN p_admin_id INT,
    IN p_price DECIMAL(15,2),
    IN p_units INT,
    IN p_start DATE,
    IN p_end DATE,
    OUT p_ipo_id INT
)
BEGIN
    INSERT INTO IPO(asset_id, ipo_price, total_units, units_sold, start_date, end_date, status, launched_by)
    VALUES(p_asset_id, p_price, p_units, 0, p_start, p_end, 'OPEN', p_admin_id);
    SET p_ipo_id = LAST_INSERT_ID();
    UPDATE ASSET SET available_units = p_units WHERE asset_id = p_asset_id;
END //

DELIMITER ;

-- ============================================================
-- TRIGGERS
-- ============================================================

DELIMITER //

-- Trigger: Auto-update IPO status based on dates
CREATE TRIGGER trg_ipo_status_check
BEFORE UPDATE ON IPO
FOR EACH ROW
BEGIN
    IF NEW.units_sold >= NEW.total_units THEN
        SET NEW.status = 'CLOSED';
    END IF;
    IF NEW.end_date < CURDATE() AND NEW.status = 'OPEN' THEN
        SET NEW.status = 'CLOSED';
    END IF;
END //

-- Trigger: Record valuation on asset price change
CREATE TRIGGER trg_asset_valuation_update
AFTER UPDATE ON ASSET
FOR EACH ROW
BEGIN
    IF OLD.current_price <> NEW.current_price THEN
        INSERT INTO VALUATION(asset_id, valuation_date, price_per_unit, total_value)
        VALUES(NEW.asset_id, CURDATE(), NEW.current_price, NEW.current_price * NEW.total_units);
    END IF;
END //

-- Trigger: Prevent negative wallet
CREATE TRIGGER trg_wallet_check
BEFORE UPDATE ON INVESTOR
FOR EACH ROW
BEGIN
    IF NEW.wallet_balance < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Wallet balance cannot go negative';
    END IF;
END //

DELIMITER ;

-- ============================================================
-- SQL FUNCTIONS
-- ============================================================

DELIMITER //

CREATE FUNCTION fn_portfolio_value(p_investor_id INT)
RETURNS DECIMAL(15,2)
READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(15,2);
    SELECT COALESCE(SUM(o.units_owned * a.current_price), 0)
    INTO v_total
    FROM OWNERSHIP o JOIN ASSET a ON o.asset_id = a.asset_id
    WHERE o.investor_id = p_investor_id;
    RETURN v_total;
END //

CREATE FUNCTION fn_portfolio_gain_pct(p_investor_id INT)
RETURNS DECIMAL(8,2)
READS SQL DATA
BEGIN
    DECLARE v_current DECIMAL(15,2);
    DECLARE v_cost DECIMAL(15,2);
    SELECT COALESCE(SUM(o.units_owned * a.current_price), 0),
           COALESCE(SUM(o.units_owned * o.avg_buy_price), 0)
    INTO v_current, v_cost
    FROM OWNERSHIP o JOIN ASSET a ON o.asset_id = a.asset_id
    WHERE o.investor_id = p_investor_id;
    IF v_cost = 0 THEN RETURN 0; END IF;
    RETURN ROUND(((v_current - v_cost) / v_cost) * 100, 2);
END //

CREATE FUNCTION fn_asset_roi(p_asset_id INT)
RETURNS DECIMAL(8,2)
READS SQL DATA
BEGIN
    DECLARE v_first_price DECIMAL(15,2);
    DECLARE v_current_price DECIMAL(15,2);
    SELECT price_per_unit INTO v_first_price FROM VALUATION
    WHERE asset_id = p_asset_id ORDER BY valuation_date ASC LIMIT 1;
    SELECT current_price INTO v_current_price FROM ASSET WHERE asset_id = p_asset_id;
    IF v_first_price IS NULL OR v_first_price = 0 THEN RETURN 0; END IF;
    RETURN ROUND(((v_current_price - v_first_price) / v_first_price) * 100, 2);
END //

DELIMITER ;

-- ============================================================
-- SEED DATA
-- ============================================================

-- Admins (password: admin123)
INSERT INTO ADMIN(username, password, name, email) VALUES
('admin1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh','Super Admin','admin1@fractional.com'),
('admin2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh','Ops Admin','admin2@fractional.com');

-- Investors (password: inv123)
INSERT INTO INVESTOR(username, password, name, email, phone, wallet_balance, kyc_verified) VALUES
('alice',  '$2a$10$8K1p/a0dR1xqM4B3oUKwDeBDdBb7RCl0Z1i5DEkQT7GVIU4cJ5Xwi','Alice Johnson','alice@mail.com','9876543210',250000.00, TRUE),
('bob',    '$2a$10$8K1p/a0dR1xqM4B3oUKwDeBDdBb7RCl0Z1i5DEkQT7GVIU4cJ5Xwi','Bob Smith','bob@mail.com','9876543211',180000.00, TRUE),
('carol',  '$2a$10$8K1p/a0dR1xqM4B3oUKwDeBDdBb7RCl0Z1i5DEkQT7GVIU4cJ5Xwi','Carol White','carol@mail.com','9876543212',320000.00, TRUE),
('dave',   '$2a$10$8K1p/a0dR1xqM4B3oUKwDeBDdBb7RCl0Z1i5DEkQT7GVIU4cJ5Xwi','Dave Brown','dave@mail.com','9876543213',90000.00, FALSE),
('eve',    '$2a$10$8K1p/a0dR1xqM4B3oUKwDeBDdBb7RCl0Z1i5DEkQT7GVIU4cJ5Xwi','Eve Davis','eve@mail.com','9876543214',450000.00, TRUE);

-- Assets
INSERT INTO ASSET(name, category, description, total_units, available_units, current_price, total_value, verification_status, listed_by_admin) VALUES
('Rare 1952 Mickey Mantle Baseball Card', 'Collectibles', 'PSA Grade 9 Rookie Card – one of only 3 known in this condition.', 1000, 400, 250.00, 250000.00, 'VERIFIED', 1),
('Vintage 1967 Ferrari 275 GTB/4', 'Luxury Cars', 'Concours-condition Ferrari, fully restored, eligible for major rallies.', 5000, 1800, 600.00, 3000000.00, 'VERIFIED', 1),
('Banksy "Girl with Balloon" Original', 'Fine Art', 'Authenticated Banksy original on canvas, exhibited in London 2019.', 2000, 700, 1500.00, 3000000.00, 'VERIFIED', 2),
('Patek Philippe Nautilus 5711/1A', 'Luxury Watches', 'Brand new sealed Patek Philippe Nautilus – discontinued reference.', 500, 200, 800.00, 400000.00, 'VERIFIED', 2),
('1959 Gibson Les Paul Standard', 'Vintage Instruments', 'All-original 1959 Les Paul in Sunburst, highly collectible.', 800, 350, 500.00, 400000.00, 'VERIFIED', 1),
('Rolex Daytona Paul Newman Ref. 6239', 'Luxury Watches', 'Iconic Paul Newman Daytona with original dial.', 400, 180, 1200.00, 480000.00, 'PENDING', 2),
('Pokémon Shadowless Charizard PSA 10', 'Collectibles', 'Holy Grail of Pokemon cards – near perfect gem mint.', 300, 120, 3000.00, 900000.00, 'VERIFIED', 1),
('Hermès Birkin 35 Himalayan Crocodile', 'Luxury Bags', 'The rarest Birkin – Himalayan Nilo Crocodile with diamond hardware.', 200, 80, 4500.00, 900000.00, 'VERIFIED', 2);

-- IPOs
INSERT INTO IPO(asset_id, ipo_price, total_units, units_sold, start_date, end_date, status, launched_by) VALUES
(1, 200.00, 600, 200, '2024-01-01', '2025-12-31', 'OPEN', 1),
(2, 550.00, 3200, 1400, '2024-02-01', '2025-12-31', 'OPEN', 1),
(3, 1200.00, 1300, 600, '2024-03-01', '2025-12-31', 'OPEN', 2),
(4, 700.00, 300, 100, '2024-04-01', '2025-12-31', 'OPEN', 2),
(5, 450.00, 450, 100, '2024-05-01', '2025-12-31', 'OPEN', 1),
(7, 2500.00, 180, 60, '2024-06-01', '2025-12-31', 'OPEN', 1),
(8, 4000.00, 120, 40, '2024-07-01', '2025-12-31', 'OPEN', 2);

-- Ownership (alice and bob own several assets)
INSERT INTO OWNERSHIP(investor_id, asset_id, units_owned, avg_buy_price) VALUES
(1, 1, 50, 200.00),   -- alice owns Mickey Mantle card
(1, 2, 100, 550.00),  -- alice owns Ferrari
(1, 3, 30, 1200.00),  -- alice owns Banksy
(2, 2, 80, 550.00),   -- bob owns Ferrari
(2, 4, 40, 700.00),   -- bob owns Patek
(3, 3, 70, 1200.00),  -- carol owns Banksy
(3, 7, 20, 2500.00),  -- carol owns Charizard
(5, 8, 30, 4000.00),  -- eve owns Birkin
(5, 1, 100, 200.00);  -- eve owns Mickey Mantle

-- Ownership History
INSERT INTO OWNERSHIP_HISTORY(investor_id, asset_id, units_change, change_type, price_per_unit, changed_at) VALUES
(1, 1, 50, 'IPO', 200.00, '2024-01-15 09:00:00'),
(1, 2, 100, 'IPO', 550.00, '2024-02-10 10:00:00'),
(1, 3, 30, 'IPO', 1200.00, '2024-03-05 11:00:00'),
(2, 2, 80, 'IPO', 550.00, '2024-02-12 09:30:00'),
(2, 4, 40, 'IPO', 700.00, '2024-04-10 10:00:00'),
(3, 3, 70, 'IPO', 1200.00, '2024-03-08 14:00:00'),
(3, 7, 20, 'IPO', 2500.00, '2024-06-10 12:00:00'),
(5, 8, 30, 'IPO', 4000.00, '2024-07-05 09:00:00'),
(5, 1, 100, 'IPO', 200.00, '2024-01-20 11:00:00');

-- Trades
INSERT INTO TRADE(asset_id, buyer_id, seller_id, units, price_per_unit, total_amount, traded_at) VALUES
(1, 1, NULL, 50, 200.00, 10000.00, '2024-01-15 09:00:00'),
(2, 1, NULL, 100, 550.00, 55000.00, '2024-02-10 10:00:00'),
(3, 1, NULL, 30, 1200.00, 36000.00, '2024-03-05 11:00:00'),
(2, 2, NULL, 80, 550.00, 44000.00, '2024-02-12 09:30:00'),
(4, 2, NULL, 40, 700.00, 28000.00, '2024-04-10 10:00:00'),
(3, 3, NULL, 70, 1200.00, 84000.00, '2024-03-08 14:00:00'),
(7, 3, NULL, 20, 2500.00, 50000.00, '2024-06-10 12:00:00'),
(8, 5, NULL, 30, 4000.00, 120000.00, '2024-07-05 09:00:00'),
(1, 5, NULL, 100, 200.00, 20000.00, '2024-01-20 11:00:00');

-- Valuation history (monthly snapshots)
INSERT INTO VALUATION(asset_id, valuation_date, price_per_unit, total_value, recorded_by) VALUES
-- Mickey Mantle (asset 1)
(1, '2024-01-01', 200.00, 200000.00, 1),
(1, '2024-04-01', 215.00, 215000.00, 1),
(1, '2024-07-01', 235.00, 235000.00, 1),
(1, '2024-10-01', 242.00, 242000.00, 1),
(1, '2025-01-01', 250.00, 250000.00, 1),
-- Ferrari (asset 2)
(2, '2024-02-01', 550.00, 2750000.00, 1),
(2, '2024-05-01', 565.00, 2825000.00, 1),
(2, '2024-08-01', 580.00, 2900000.00, 1),
(2, '2024-11-01', 592.00, 2960000.00, 1),
(2, '2025-01-01', 600.00, 3000000.00, 1),
-- Banksy (asset 3)
(3, '2024-03-01', 1200.00, 2400000.00, 2),
(3, '2024-06-01', 1320.00, 2640000.00, 2),
(3, '2024-09-01', 1410.00, 2820000.00, 2),
(3, '2024-12-01', 1480.00, 2960000.00, 2),
(3, '2025-01-01', 1500.00, 3000000.00, 2),
-- Patek (asset 4)
(4, '2024-04-01', 700.00, 350000.00, 2),
(4, '2024-07-01', 740.00, 370000.00, 2),
(4, '2024-10-01', 780.00, 390000.00, 2),
(4, '2025-01-01', 800.00, 400000.00, 2),
-- Gibson (asset 5)
(5, '2024-05-01', 450.00, 360000.00, 1),
(5, '2024-08-01', 470.00, 376000.00, 1),
(5, '2025-01-01', 500.00, 400000.00, 1),
-- Charizard (asset 7)
(7, '2024-06-01', 2500.00, 750000.00, 1),
(7, '2024-10-01', 2800.00, 840000.00, 1),
(7, '2025-01-01', 3000.00, 900000.00, 1),
-- Birkin (asset 8)
(8, '2024-07-01', 4000.00, 800000.00, 2),
(8, '2025-01-01', 4500.00, 900000.00, 2);

-- Update wallet balances after purchases
UPDATE INVESTOR SET wallet_balance = 250000.00 - (50*200 + 100*550 + 30*1200) WHERE investor_id = 1;  -- alice
UPDATE INVESTOR SET wallet_balance = 180000.00 - (80*550 + 40*700) WHERE investor_id = 2;              -- bob
UPDATE INVESTOR SET wallet_balance = 320000.00 - (70*1200 + 20*2500) WHERE investor_id = 3;            -- carol
UPDATE INVESTOR SET wallet_balance = 450000.00 - (30*4000 + 100*200) WHERE investor_id = 5;            -- eve

-- Pending trade orders
INSERT INTO TRADE_ORDER(investor_id, asset_id, order_type, units, price_per_unit, status) VALUES
(2, 1, 'BUY', 20, 245.00, 'PENDING'),
(1, 3, 'SELL', 10, 1520.00, 'PENDING'),
(4, 5, 'BUY', 15, 495.00, 'PENDING'),
(3, 2, 'SELL', 25, 610.00, 'PENDING');
