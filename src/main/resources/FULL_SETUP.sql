-- ================================================================
-- FRACTIONAL OWNERSHIP — COMPLETE SETUP SCRIPT
-- Run this ONCE in your TiDB/MySQL console to set up everything.
-- After this, run SeedUserPasswords.java from your IDE.
-- ================================================================

CREATE DATABASE IF NOT EXISTS fractional_ownership_db;
USE fractional_ownership_db;

SET FOREIGN_KEY_CHECKS = 0;

-- ── Tables ───────────────────────────────────────────────────────
DROP TABLE IF EXISTS OWNERSHIP_HISTORY;
DROP TABLE IF EXISTS WALLET_TRANSACTION;
DROP TABLE IF EXISTS TRADE;
DROP TABLE IF EXISTS TRADE_ORDER;
DROP TABLE IF EXISTS OWNERSHIP;
DROP TABLE IF EXISTS VALUATION;
DROP TABLE IF EXISTS IPO;
DROP TABLE IF EXISTS ASSET;
DROP TABLE IF EXISTS INVESTOR;
DROP TABLE IF EXISTS ADMIN;
DROP TABLE IF EXISTS user_auth;

CREATE TABLE ADMIN (
  admin_id   INT NOT NULL,
  name       VARCHAR(100) DEFAULT NULL,
  email      VARCHAR(100) DEFAULT NULL,
  role       VARCHAR(50)  DEFAULT NULL,
  PRIMARY KEY (admin_id),
  UNIQUE KEY email (email)
);

CREATE TABLE INVESTOR (
  investor_id       INT NOT NULL,
  investor_name     VARCHAR(100) DEFAULT NULL,
  email             VARCHAR(100) DEFAULT NULL,
  phone             VARCHAR(20)  DEFAULT NULL,
  registration_date DATE         DEFAULT NULL,
  wallet_balance    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (investor_id),
  UNIQUE KEY email (email)
);

CREATE TABLE ASSET (
  asset_id               INT NOT NULL,
  asset_name             VARCHAR(100) DEFAULT NULL,
  category               VARCHAR(50)  DEFAULT NULL,
  description            TEXT,
  storage_location       VARCHAR(100) DEFAULT NULL,
  verification_reference VARCHAR(100) DEFAULT NULL,
  verification_status    VARCHAR(20)  DEFAULT NULL,
  verified_by            INT          DEFAULT NULL,
  PRIMARY KEY (asset_id),
  KEY idx_asset_category (category),
  CONSTRAINT ASSET_ibfk_1 FOREIGN KEY (verified_by) REFERENCES ADMIN (admin_id)
);

CREATE TABLE IPO (
  ipo_id          INT NOT NULL,
  asset_id        INT DEFAULT NULL,
  total_units     INT DEFAULT NULL,
  price_per_unit  DECIMAL(10,2) DEFAULT NULL,
  ipo_start_date  DATE DEFAULT NULL,
  ipo_end_date    DATE DEFAULT NULL,
  lock_in_period  INT DEFAULT NULL,
  PRIMARY KEY (ipo_id),
  UNIQUE KEY asset_id (asset_id),
  CONSTRAINT IPO_ibfk_1 FOREIGN KEY (asset_id) REFERENCES ASSET (asset_id)
);

CREATE TABLE VALUATION (
  valuation_id     INT NOT NULL,
  asset_id         INT DEFAULT NULL,
  valuation_amount DECIMAL(12,2) DEFAULT NULL,
  valuation_date   DATE DEFAULT NULL,
  PRIMARY KEY (valuation_id),
  UNIQUE KEY asset_id (asset_id, valuation_date),
  KEY idx_valuation_date (valuation_date),
  CONSTRAINT VALUATION_ibfk_1 FOREIGN KEY (asset_id) REFERENCES ASSET (asset_id)
);

CREATE TABLE OWNERSHIP (
  investor_id INT NOT NULL,
  asset_id    INT NOT NULL,
  units_held  INT DEFAULT NULL,
  PRIMARY KEY (investor_id, asset_id),
  KEY asset_id (asset_id),
  CONSTRAINT OWNERSHIP_ibfk_1 FOREIGN KEY (investor_id) REFERENCES INVESTOR (investor_id),
  CONSTRAINT OWNERSHIP_ibfk_2 FOREIGN KEY (asset_id)    REFERENCES ASSET (asset_id),
  CONSTRAINT OWNERSHIP_chk_1 CHECK (units_held >= 0)
);

CREATE TABLE TRADE_ORDER (
  order_id    INT NOT NULL,
  investor_id INT DEFAULT NULL,
  asset_id    INT DEFAULT NULL,
  order_type  VARCHAR(10)   DEFAULT NULL,
  price       DECIMAL(10,2) DEFAULT NULL,
  units       INT           DEFAULT NULL,
  order_date  DATE          DEFAULT NULL,
  status      VARCHAR(20)   DEFAULT NULL,
  PRIMARY KEY (order_id),
  KEY investor_id (investor_id),
  KEY asset_id (asset_id),
  KEY idx_order_date (order_date),
  CONSTRAINT TRADE_ORDER_ibfk_1 FOREIGN KEY (investor_id) REFERENCES INVESTOR (investor_id),
  CONSTRAINT TRADE_ORDER_ibfk_2 FOREIGN KEY (asset_id)    REFERENCES ASSET (asset_id)
);

CREATE TABLE TRADE (
  trade_id      INT NOT NULL,
  trade_price   DECIMAL(10,2) DEFAULT NULL,
  trade_units   INT           DEFAULT NULL,
  trade_date    DATE          DEFAULT NULL,
  buy_order_id  INT           DEFAULT NULL,
  sell_order_id INT           DEFAULT NULL,
  PRIMARY KEY (trade_id),
  KEY buy_order_id  (buy_order_id),
  KEY sell_order_id (sell_order_id),
  KEY idx_trade_date (trade_date),
  CONSTRAINT TRADE_ibfk_1 FOREIGN KEY (buy_order_id)  REFERENCES TRADE_ORDER (order_id),
  CONSTRAINT TRADE_ibfk_2 FOREIGN KEY (sell_order_id) REFERENCES TRADE_ORDER (order_id)
);

CREATE TABLE OWNERSHIP_HISTORY (
  history_id  INT NOT NULL AUTO_INCREMENT,
  investor_id INT DEFAULT NULL,
  asset_id    INT DEFAULT NULL,
  units_before INT DEFAULT NULL,
  units_after  INT DEFAULT NULL,
  change_date  DATE DEFAULT NULL,
  change_type  VARCHAR(20) DEFAULT NULL,
  trade_id     INT DEFAULT NULL,
  ipo_id       INT DEFAULT NULL,
  PRIMARY KEY (history_id),
  KEY investor_id (investor_id),
  KEY asset_id    (asset_id),
  CONSTRAINT OWNERSHIP_HISTORY_ibfk_1 FOREIGN KEY (investor_id) REFERENCES INVESTOR (investor_id),
  CONSTRAINT OWNERSHIP_HISTORY_ibfk_2 FOREIGN KEY (asset_id)    REFERENCES ASSET (asset_id)
);

CREATE TABLE WALLET_TRANSACTION (
  transaction_id   INT NOT NULL AUTO_INCREMENT,
  investor_id      INT NOT NULL,
  amount           DECIMAL(15,2) NOT NULL,
  transaction_type VARCHAR(50) NOT NULL,
  transfer_category VARCHAR(100) DEFAULT NULL,
  transaction_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (transaction_id),
  KEY investor_id (investor_id),
  CONSTRAINT WALLET_TRANSACTION_ibfk_1 FOREIGN KEY (investor_id) REFERENCES INVESTOR (investor_id)
);

CREATE TABLE user_auth (
  auth_id          INT AUTO_INCREMENT PRIMARY KEY,
  email            VARCHAR(100) NOT NULL UNIQUE,
  password_hash    VARCHAR(255) NOT NULL DEFAULT 'UNSET',
  user_type        ENUM('ADMIN','INVESTOR') NOT NULL,
  linked_id        INT NOT NULL,
  status           ENUM('ACTIVE','INACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
  last_login       DATETIME NULL,
  password_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  password_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET FOREIGN_KEY_CHECKS = 1;

-- ── Seed Data ─────────────────────────────────────────────────────
INSERT INTO ADMIN VALUES
(1,'Ananya Rao','ananya@platform.com','Verifier'),
(2,'Raghav Menon','raghav@platform.com','Verifier'),
(3,'Priya Shah','priya@platform.com','Verifier'),
(4,'Vikram Sethi','vikram@platform.com','Verifier'),
(5,'Neha Kapoor','neha@platform.com','Verifier'),
(6,'Aditya Nair','aditya@platform.com','Verifier'),
(7,'Sonal Gupta','sonal@platform.com','Verifier'),
(8,'Kunal Arora','kunal@platform.com','Verifier'),
(9,'Ira Malhotra','ira@platform.com','Verifier'),
(10,'Sameer Khan','sameer@platform.com','Verifier'),
(11,'Divya Iyer','divya@platform.com','Verifier'),
(12,'Manav Joshi','manav@platform.com','Verifier'),
(13,'Aisha Qureshi','aisha@platform.com','Verifier'),
(14,'Nikhil Bansal','nikhil@platform.com','Verifier'),
(15,'Ritu Sharma','ritu@platform.com','Verifier');

INSERT INTO INVESTOR (investor_id, investor_name, email, phone, registration_date, wallet_balance) VALUES
(1,'Aman Gupta','aman@gmail.com','9810000001','2023-11-12',5430.00),
(2,'Riya Malhotra','riya@gmail.com','9810000002','2023-12-01',0.00),
(3,'Karan Mehta','karan@gmail.com','9810000003','2024-01-15',0.00),
(4,'Sneha Iyer','sneha@gmail.com','9810000004','2024-02-03',0.00),
(5,'Arjun Verma','arjun@gmail.com','9810000005','2024-03-20',0.00),
(6,'Meera Jain','meera@gmail.com','9810000006','2024-04-05',0.00),
(7,'Rahul Bose','rahul@gmail.com','9810000007','2024-05-11',0.00),
(8,'Tanya Roy','tanya@gmail.com','9810000008','2024-06-18',0.00),
(9,'Dev Khanna','dev@gmail.com','9810000009','2024-07-02',0.00),
(10,'Ishita Singh','ishita@gmail.com','9810000010','2024-08-10',0.00),
(11,'Harsh Patel','harsh@gmail.com','9810000011','2024-09-01',0.00),
(12,'Neeraj Sood','neeraj@gmail.com','9810000012','2024-09-20',0.00),
(13,'Simran Kaur','simran@gmail.com','9810000013','2024-10-05',0.00),
(14,'Rohit Das','rohit@gmail.com','9810000014','2024-11-12',0.00),
(15,'Pooja Nair','pooja@gmail.com','9810000015','2024-12-01',0.00);

INSERT INTO ASSET (asset_id, asset_name, category, description, storage_location, verification_reference, verification_status, verified_by) VALUES
(1,'Picasso Sketch','Art','1962 pencil sketch','Vault-A1','CERT-A1','Verified',1),
(2,'Rolex Daytona 1984','Watch','Vintage Rolex','Vault-B2','CERT-W2','Verified',2),
(3,'Bordeaux 1990 Reserve','Wine','French wine reserve','Climate-V1','CERT-W3','Verified',3),
(4,'Banksy Print','Art','Signed street art','Vault-A3','CERT-A4','Verified',4),
(5,'Patek Philippe 1975','Watch','Swiss luxury watch','Vault-B4','CERT-W5','Verified',5),
(6,'Macallan 1926','Wine','Rare whisky','Climate-V2','CERT-W6','Verified',6),
(7,'Monet Landscape','Art','Oil painting 1880','Vault-A7','CERT-A7','Verified',7),
(8,'Omega Speedmaster','Watch','Moon edition','Vault-B8','CERT-W8','Verified',8),
(9,'Dom Perignon 1988','Wine','Vintage champagne','Climate-V3','CERT-W9','Verified',9),
(10,'Van Gogh Replica','Art','Museum replica','Vault-A10','CERT-A10','Verified',10),
(11,'Richard Mille RM011','Watch','Luxury sports watch','Vault-B11','CERT-W11','Verified',11),
(12,'Screaming Eagle 1992','Wine','Rare California wine','Climate-V4','CERT-W12','Verified',12),
(13,'Andy Warhol Poster','Art','Pop art original','Vault-A13','CERT-A13','Verified',13),
(14,'Audemars Piguet Royal Oak','Watch','Steel sports watch','Vault-B14','CERT-W14','Verified',14),
(15,'Chateau Lafite 2000','Wine','French grand cru','Climate-V5','CERT-W15','Verified',15);

INSERT INTO IPO VALUES
(1,1,100,12000.00,'2024-01-10','2024-01-20',30),
(2,2,80,8500.00,'2024-02-05','2024-02-15',45),
(3,3,150,6000.00,'2024-03-01','2024-03-10',30),
(4,4,120,9500.00,'2024-04-01','2024-04-12',60),
(5,5,60,15000.00,'2024-05-10','2024-05-20',30),
(6,6,200,7000.00,'2024-06-01','2024-06-12',90),
(7,7,90,11000.00,'2024-07-01','2024-07-10',30),
(8,8,70,9000.00,'2024-08-01','2024-08-10',45),
(9,9,130,5000.00,'2024-09-01','2024-09-12',30),
(10,10,50,4000.00,'2024-10-01','2024-10-10',30),
(11,11,75,20000.00,'2024-11-01','2024-11-12',60),
(12,12,140,6500.00,'2024-12-01','2024-12-10',30),
(13,13,95,10500.00,'2025-01-01','2025-01-10',45),
(14,14,85,17000.00,'2025-02-01','2025-02-10',30),
(15,15,160,5500.00,'2025-03-01','2025-03-10',30);

INSERT INTO VALUATION VALUES
(1,1,1250000.00,'2024-02-01'),(2,1,1300000.00,'2024-06-01'),
(3,2,700000.00,'2024-03-01'),(4,2,720000.00,'2024-07-01'),
(5,3,900000.00,'2024-04-01'),(6,3,920000.00,'2024-08-01'),
(7,4,1150000.00,'2024-05-01'),(8,5,950000.00,'2024-06-15'),
(9,6,1600000.00,'2024-07-20'),(10,7,990000.00,'2024-08-10'),
(11,8,650000.00,'2024-09-15'),(12,9,750000.00,'2024-10-01'),
(13,10,220000.00,'2024-11-01'),(14,11,1800000.00,'2024-12-01'),
(15,12,1000000.00,'2025-01-01');

INSERT INTO OWNERSHIP VALUES
(1,1,40),(1,4,70),(2,1,30),(2,3,60),(2,11,75),(3,1,30),(3,5,60),(3,12,70),
(4,2,50),(4,7,90),(4,12,70),(5,2,30),(5,6,100),(6,3,50),(6,8,40),(7,3,40),
(7,8,30),(8,4,50),(8,9,60),(9,6,100),(9,10,50),(10,9,70),(11,13,50),
(12,13,45),(13,14,85),(14,15,80),(15,15,80);

INSERT INTO TRADE_ORDER VALUES
(1,1,1,'SELL',13000.00,10,'2024-03-01','MATCHED'),(2,4,2,'SELL',9000.00,10,'2024-04-01','MATCHED'),
(3,2,3,'BUY',6500.00,20,'2024-05-01','OPEN'),(4,8,4,'BUY',9800.00,15,'2024-06-01','MATCHED'),
(5,6,8,'SELL',9500.00,5,'2024-09-01','OPEN'),(6,3,12,'BUY',7000.00,10,'2025-01-15','OPEN'),
(7,11,13,'SELL',11000.00,5,'2025-02-15','OPEN'),(8,12,13,'BUY',11500.00,5,'2025-02-16','MATCHED'),
(9,14,15,'SELL',6000.00,10,'2025-04-01','OPEN'),(10,15,15,'BUY',6200.00,10,'2025-04-02','MATCHED'),
(11,5,6,'SELL',7200.00,15,'2024-10-01','MATCHED'),(12,9,6,'BUY',7300.00,15,'2024-10-02','MATCHED'),
(13,7,3,'SELL',6800.00,10,'2024-09-01','OPEN'),(14,10,9,'BUY',5200.00,20,'2024-11-01','OPEN'),
(15,8,9,'SELL',5300.00,20,'2024-11-02','MATCHED');

INSERT INTO TRADE VALUES
(1,13000.00,10,'2024-03-02',1,1),(2,9000.00,10,'2024-04-02',2,2),
(3,9800.00,15,'2024-06-02',4,4),(4,11500.00,5,'2025-02-16',8,7),
(5,6200.00,10,'2025-04-02',10,9),(6,7300.00,15,'2024-10-02',12,11);

INSERT INTO OWNERSHIP_HISTORY VALUES
(1,1,1,0,40,'2024-01-20','IPO',NULL,NULL),
(2,2,1,0,30,'2024-01-20','IPO',NULL,NULL),
(3,3,1,0,30,'2024-01-20','IPO',NULL,NULL),
(4,1,1,40,30,'2024-03-02','TRADE',1,NULL),
(5,4,2,50,40,'2024-04-02','TRADE',2,NULL),
(6,8,4,50,65,'2024-06-02','TRADE',3,NULL);

INSERT INTO WALLET_TRANSACTION (investor_id, amount, transaction_type, transfer_category, transaction_date) VALUES
(1,100000.00,'DEPOSIT','Bank Transfer','2024-01-15 10:00:00'),
(1,5000.00,'DIVIDEND','Yield Payout','2024-06-01 09:00:00'),
(1,1950.00,'DIVIDEND','Yield Payout','2024-12-01 09:00:00'),
(1,-80000.00,'WITHDRAWAL','Bank Transfer','2025-01-10 14:00:00');

-- ── Triggers ──────────────────────────────────────────────────────
DROP TRIGGER IF EXISTS trg_prevent_negative_wallet_balance;
DROP TRIGGER IF EXISTS trg_update_wallet_balance_after_transaction;
DROP TRIGGER IF EXISTS trg_update_order_status_after_trade;
DROP TRIGGER IF EXISTS trg_log_ownership_history_on_update;
DROP TRIGGER IF EXISTS trg_log_ownership_history_on_insert;

DELIMITER $$

CREATE TRIGGER trg_prevent_negative_wallet_balance
BEFORE INSERT ON WALLET_TRANSACTION FOR EACH ROW
BEGIN
    DECLARE current_balance DECIMAL(15,2);
    SELECT wallet_balance INTO current_balance FROM INVESTOR WHERE investor_id = NEW.investor_id;
    IF (current_balance + NEW.amount) < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient wallet balance for this transaction';
    END IF;
END$$

CREATE TRIGGER trg_update_wallet_balance_after_transaction
AFTER INSERT ON WALLET_TRANSACTION FOR EACH ROW
BEGIN
    UPDATE INVESTOR SET wallet_balance = wallet_balance + NEW.amount WHERE investor_id = NEW.investor_id;
END$$

CREATE TRIGGER trg_update_order_status_after_trade
AFTER INSERT ON TRADE FOR EACH ROW
BEGIN
    IF NEW.buy_order_id IS NOT NULL THEN
        UPDATE TRADE_ORDER SET status = 'MATCHED' WHERE order_id = NEW.buy_order_id;
    END IF;
    IF NEW.sell_order_id IS NOT NULL THEN
        UPDATE TRADE_ORDER SET status = 'MATCHED' WHERE order_id = NEW.sell_order_id;
    END IF;
END$$

CREATE TRIGGER trg_log_ownership_history_on_update
AFTER UPDATE ON OWNERSHIP FOR EACH ROW
BEGIN
    IF OLD.units_held <> NEW.units_held THEN
        INSERT INTO OWNERSHIP_HISTORY (investor_id, asset_id, units_before, units_after, change_date, change_type)
        VALUES (NEW.investor_id, NEW.asset_id, OLD.units_held, NEW.units_held, CURDATE(), 'HOLDING_UPDATE');
    END IF;
END$$

CREATE TRIGGER trg_log_ownership_history_on_insert
AFTER INSERT ON OWNERSHIP FOR EACH ROW
BEGIN
    INSERT INTO OWNERSHIP_HISTORY (investor_id, asset_id, units_before, units_after, change_date, change_type)
    VALUES (NEW.investor_id, NEW.asset_id, 0, NEW.units_held, CURDATE(), 'INITIAL_ALLOCATION');
END$$

DELIMITER ;

-- ── Auth Table ────────────────────────────────────────────────────
INSERT INTO user_auth (email, password_hash, user_type, linked_id, status)
SELECT email, 'UNSET', 'INVESTOR', investor_id, 'ACTIVE' FROM investor;

INSERT INTO user_auth (email, password_hash, user_type, linked_id, status)
SELECT email, 'UNSET', 'ADMIN', admin_id, 'ACTIVE' FROM admin;

SELECT CONCAT('Setup complete! ', COUNT(*), ' users in user_auth.') AS status FROM user_auth;
SELECT user_type, COUNT(*) AS count FROM user_auth GROUP BY user_type;

-- !! NOW run SeedUserPasswords.java from your IDE to set password hashes !!
