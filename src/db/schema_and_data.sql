--CREATE DATABASE IF NOT EXISTS fractional_ownership_db;
--USE fractional_ownership_db;

SET FOREIGN_KEY_CHECKS = 0;

--
-- Table structure for table `ADMIN`
--
DROP TABLE IF EXISTS `ADMIN`;
CREATE TABLE `ADMIN` (
  `admin_id` int NOT NULL,
  `admin_name` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `role` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `ADMIN`
--
INSERT INTO `ADMIN` (admin_id, admin_name, email, role) VALUES (1,'Ananya Rao','ananya@platform.com','Verifier'),(2,'Raghav Menon','raghav@platform.com','Verifier'),(3,'Priya Shah','priya@platform.com','Verifier'),(4,'Vikram Sethi','vikram@platform.com','Verifier'),(5,'Neha Kapoor','neha@platform.com','Verifier'),(6,'Aditya Nair','aditya@platform.com','Verifier'),(7,'Sonal Gupta','sonal@platform.com','Verifier'),(8,'Kunal Arora','kunal@platform.com','Verifier'),(9,'Ira Malhotra','ira@platform.com','Verifier'),(10,'Sameer Khan','sameer@platform.com','Verifier'),(11,'Divya Iyer','divya@platform.com','Verifier'),(12,'Manav Joshi','manav@platform.com','Verifier'),(13,'Aisha Qureshi','aisha@platform.com','Verifier'),(14,'Nikhil Bansal','nikhil@platform.com','Verifier'),(15,'Ritu Sharma','ritu@platform.com','Verifier');

--
-- Table structure for table `ASSET`
--
DROP TABLE IF EXISTS `ASSET`;
CREATE TABLE `ASSET` (
  `asset_id` int NOT NULL,
  `asset_name` varchar(100) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `description` text,
  `storage_location` varchar(100) DEFAULT NULL,
  `verification_reference` varchar(100) DEFAULT NULL,
  `verification_status` varchar(20) DEFAULT NULL,
  `verified_by` int DEFAULT NULL,
  PRIMARY KEY (`asset_id`),
  KEY `verified_by` (`verified_by`),
  KEY `idx_asset_category` (`category`),
  CONSTRAINT `ASSET_ibfk_1` FOREIGN KEY (`verified_by`) REFERENCES `ADMIN` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `ASSET`
--
INSERT INTO `ASSET` (asset_id, asset_name, category, description, storage_location, verification_reference, verification_status, verified_by) VALUES
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


--
-- Table structure for table `INVESTOR`
--
DROP TABLE IF EXISTS `INVESTOR`;
CREATE TABLE `INVESTOR` (
  `investor_id` int NOT NULL,
  `investor_name` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `registration_date` date DEFAULT NULL,
  `wallet_balance` decimal(15,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`investor_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `INVESTOR`
--
INSERT INTO `INVESTOR` (investor_id, investor_name, email, phone, registration_date, wallet_balance) VALUES
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

--
-- Table structure for table `WALLET_TRANSACTION`
--
DROP TABLE IF EXISTS `WALLET_TRANSACTION`;
CREATE TABLE `WALLET_TRANSACTION` (
  `transaction_id` int NOT NULL AUTO_INCREMENT,
  `investor_id` int NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `transaction_type` varchar(50) NOT NULL,
  `transfer_category` varchar(100) DEFAULT NULL,
  `transaction_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`transaction_id`),
  KEY `investor_id` (`investor_id`),
  CONSTRAINT `WALLET_TRANSACTION_ibfk_1` FOREIGN KEY (`investor_id`)
    REFERENCES `INVESTOR` (`investor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `WALLET_TRANSACTION`
--
INSERT INTO `WALLET_TRANSACTION` (investor_id, amount, transaction_type, transfer_category, transaction_date) VALUES
(1, 1000.00, 'DEPOSIT',        'Bank Transfer',          '2026-03-16 20:50:36'),
(1, 1000.00, 'DEPOSIT',        'Bank Transfer',          '2026-03-16 20:51:21'),
(1,   15.50, 'DIVIDEND',       'Yield Payout',           '2026-03-16 20:51:21'),
(1, -240.00, 'ASSET_PURCHASE', 'Secondary Market Order', '2026-03-16 20:51:21');

--
-- Table structure for table `IPO`
--
DROP TABLE IF EXISTS `IPO`;
CREATE TABLE `IPO` (
  `ipo_id` int NOT NULL,
  `asset_id` int DEFAULT NULL,
  `total_units` int DEFAULT NULL,
  `price_per_unit` decimal(10,2) DEFAULT NULL,
  `ipo_start_date` date DEFAULT NULL,
  `ipo_end_date` date DEFAULT NULL,
  `lock_in_period` int DEFAULT NULL,
  `units_sold` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`ipo_id`),
  UNIQUE KEY `asset_id` (`asset_id`),
  CONSTRAINT `IPO_ibfk_1` FOREIGN KEY (`asset_id`) REFERENCES `ASSET` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `IPO`
--
INSERT INTO `IPO` (
    ipo_id, asset_id, total_units, price_per_unit,
    ipo_start_date, ipo_end_date, lock_in_period, units_sold
) VALUES
(1,1,100,12000.00,'2024-01-10','2024-01-20',30,100),
(2,2,80,8500.00,'2024-02-05','2024-02-15',45,0),
(3,3,150,6000.00,'2024-03-01','2024-03-10',30,0),
(4,4,120,9500.00,'2024-04-01','2024-04-12',60,0),
(5,5,60,15000.00,'2024-05-10','2024-05-20',30,0),
(6,6,200,7000.00,'2024-06-01','2024-06-12',90,0),
(7,7,90,11000.00,'2024-07-01','2024-07-10',30,0),
(8,8,70,9000.00,'2024-08-01','2024-08-10',45,0),
(9,9,130,5000.00,'2024-09-01','2024-09-12',30,0),
(10,10,50,4000.00,'2024-10-01','2024-10-10',30,0),
(11,11,75,20000.00,'2024-11-01','2024-11-12',60,0),
(12,12,140,6500.00,'2024-12-01','2024-12-10',30,0),
(13,13,95,10500.00,'2025-01-01','2025-01-10',45,95),
(14,14,85,17000.00,'2025-02-01','2025-02-10',30,85),
(15,15,160,5500.00,'2025-03-01','2025-03-10',30,160);

--
-- Table structure for table `OWNERSHIP`
--
DROP TABLE IF EXISTS `OWNERSHIP`;
CREATE TABLE `OWNERSHIP` (
  `investor_id` int NOT NULL,
  `asset_id` int NOT NULL,
  `units_held` int DEFAULT NULL,
  PRIMARY KEY (`investor_id`,`asset_id`),
  KEY `asset_id` (`asset_id`),
  CONSTRAINT `OWNERSHIP_ibfk_1` FOREIGN KEY (`investor_id`) REFERENCES `INVESTOR` (`investor_id`),
  CONSTRAINT `OWNERSHIP_ibfk_2` FOREIGN KEY (`asset_id`) REFERENCES `ASSET` (`asset_id`),
  CONSTRAINT `OWNERSHIP_chk_1` CHECK ((`units_held` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `OWNERSHIP`
--
INSERT INTO `OWNERSHIP` VALUES (1,1,40),(1,4,70),(2,1,30),(2,3,60),(2,11,75),(3,1,30),(3,5,60),(3,12,70),(4,2,50),(4,7,90),(4,12,70),(5,2,30),(5,6,100),(6,3,50),(6,8,40),(7,3,40),(7,8,30),(8,4,50),(8,9,60),(9,6,100),(9,10,50),(10,9,70),(11,13,50),(12,13,45),(13,14,85),(14,15,80),(15,15,80);

--
-- Table structure for table `TRADE_ORDER`
--
DROP TABLE IF EXISTS `TRADE_ORDER`;
CREATE TABLE `TRADE_ORDER` (
  `order_id` int NOT NULL,
  `investor_id` int DEFAULT NULL,
  `asset_id` int DEFAULT NULL,
  `order_type` varchar(10) DEFAULT NULL,
  `price` decimal(10,2) DEFAULT NULL,
  `units` int DEFAULT NULL,
  `order_date` date DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `investor_id` (`investor_id`),
  KEY `asset_id` (`asset_id`),
  KEY `idx_order_date` (`order_date`),
  CONSTRAINT `TRADE_ORDER_ibfk_1` FOREIGN KEY (`investor_id`) REFERENCES `INVESTOR` (`investor_id`),
  CONSTRAINT `TRADE_ORDER_ibfk_2` FOREIGN KEY (`asset_id`) REFERENCES `ASSET` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `TRADE_ORDER`
--
INSERT INTO `TRADE_ORDER` VALUES (1,1,1,'SELL',13000.00,10,'2024-03-01','MATCHED'),(2,4,2,'SELL',9000.00,10,'2024-04-01','MATCHED'),(3,2,3,'BUY',6500.00,20,'2024-05-01','OPEN'),(4,8,4,'BUY',9800.00,15,'2024-06-01','MATCHED'),(5,6,8,'SELL',9500.00,5,'2024-09-01','OPEN'),(6,3,12,'BUY',7000.00,10,'2025-01-15','OPEN'),(7,11,13,'SELL',11000.00,5,'2025-02-15','OPEN'),(8,12,13,'BUY',11500.00,5,'2025-02-16','MATCHED'),(9,14,15,'SELL',6000.00,10,'2025-04-01','OPEN'),(10,15,15,'BUY',6200.00,10,'2025-04-02','MATCHED'),(11,5,6,'SELL',7200.00,15,'2024-10-01','MATCHED'),(12,9,6,'BUY',7300.00,15,'2024-10-02','MATCHED'),(13,7,3,'SELL',6800.00,10,'2024-09-01','OPEN'),(14,10,9,'BUY',5200.00,20,'2024-11-01','OPEN'),(15,8,9,'SELL',5300.00,20,'2024-11-02','MATCHED');

--
-- Table structure for table `TRADE`
--
DROP TABLE IF EXISTS `TRADE`;
CREATE TABLE `TRADE` (
  `trade_id` int NOT NULL,
  `trade_price` decimal(10,2) DEFAULT NULL,
  `trade_units` int DEFAULT NULL,
  `trade_date` date DEFAULT NULL,
  `buy_order_id` int DEFAULT NULL,
  `sell_order_id` int DEFAULT NULL,
  PRIMARY KEY (`trade_id`),
  KEY `buy_order_id` (`buy_order_id`),
  KEY `sell_order_id` (`sell_order_id`),
  KEY `idx_trade_date` (`trade_date`),
  CONSTRAINT `TRADE_ibfk_1` FOREIGN KEY (`buy_order_id`) REFERENCES `TRADE_ORDER` (`order_id`),
  CONSTRAINT `TRADE_ibfk_2` FOREIGN KEY (`sell_order_id`) REFERENCES `TRADE_ORDER` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Dumping data for table `TRADE`
--
INSERT INTO `TRADE` VALUES (1,13000.00,10,'2024-03-02',1,1),(2,9000.00,10,'2024-04-02',2,2),(3,9800.00,15,'2024-06-02',4,4),(4,11500.00,5,'2025-02-16',8,7),(5,6200.00,10,'2025-04-02',10,9),(6,7300.00,15,'2024-10-02',12,11);

--
-- Table structure for table `OWNERSHIP_HISTORY`
--
DROP TABLE IF EXISTS `OWNERSHIP_HISTORY`;
CREATE TABLE `OWNERSHIP_HISTORY` (
  `history_id` int NOT NULL AUTO_INCREMENT,
  `investor_id` int DEFAULT NULL,
  `asset_id` int DEFAULT NULL,
  `units_before` int DEFAULT NULL,
  `units_after` int DEFAULT NULL,
  `change_date` date DEFAULT NULL,
  `change_type` varchar(20) DEFAULT NULL,
  `trade_id` int DEFAULT NULL,
  `ipo_id` int DEFAULT NULL,
  PRIMARY KEY (`history_id`),
  KEY `investor_id` (`investor_id`),
  KEY `asset_id` (`asset_id`),
  KEY `trade_id` (`trade_id`),
  KEY `ipo_id` (`ipo_id`),
  CONSTRAINT `OWNERSHIP_HISTORY_ibfk_1` FOREIGN KEY (`investor_id`) REFERENCES `INVESTOR` (`investor_id`),
  CONSTRAINT `OWNERSHIP_HISTORY_ibfk_2` FOREIGN KEY (`asset_id`) REFERENCES `ASSET` (`asset_id`),
  CONSTRAINT `OWNERSHIP_HISTORY_ibfk_3` FOREIGN KEY (`trade_id`) REFERENCES `TRADE` (`trade_id`),
  CONSTRAINT `OWNERSHIP_HISTORY_ibfk_4` FOREIGN KEY (`ipo_id`) REFERENCES `IPO` (`ipo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `OWNERSHIP_HISTORY`
--
INSERT INTO `OWNERSHIP_HISTORY` VALUES (1,1,1,0,40,'2024-01-20','IPO',NULL,1),(2,2,1,0,30,'2024-01-20','IPO',NULL,1),(3,3,1,0,30,'2024-01-20','IPO',NULL,1),(4,1,1,40,30,'2024-03-02','TRADE',1,NULL),(5,4,2,50,40,'2024-04-02','TRADE',2,NULL),(6,8,4,50,65,'2024-06-02','TRADE',3,NULL),(7,11,13,50,45,'2025-02-16','TRADE',4,NULL),(8,15,15,80,90,'2025-04-02','TRADE',5,NULL),(9,5,6,100,85,'2024-10-02','TRADE',6,NULL),(10,9,6,100,115,'2024-10-02','TRADE',6,NULL),(11,13,14,0,85,'2025-02-10','IPO',NULL,14),(12,14,15,0,80,'2025-03-10','IPO',NULL,15),(13,15,15,0,80,'2025-03-10','IPO',NULL,15),(14,11,13,0,50,'2025-01-10','IPO',NULL,13),(15,12,13,0,45,'2025-01-10','IPO',NULL,13);

--
-- Table structure for table `VALUATION`
--
DROP TABLE IF EXISTS `VALUATION`;
CREATE TABLE `VALUATION` (
  `valuation_id` int NOT NULL,
  `asset_id` int DEFAULT NULL,
  `valuation_amount` decimal(12,2) DEFAULT NULL,
  `valuation_date` date DEFAULT NULL,
  PRIMARY KEY (`valuation_id`),
  UNIQUE KEY `asset_id` (`asset_id`,`valuation_date`),
  KEY `idx_valuation_date` (`valuation_date`),
  CONSTRAINT `VALUATION_ibfk_1` FOREIGN KEY (`asset_id`) REFERENCES `ASSET` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `VALUATION`
--
INSERT INTO `VALUATION` VALUES (1,1,1250000.00,'2024-02-01'),(2,1,1300000.00,'2024-06-01'),(3,2,700000.00,'2024-03-01'),(4,2,720000.00,'2024-07-01'),(5,3,900000.00,'2024-04-01'),(6,3,920000.00,'2024-08-01'),(7,4,1150000.00,'2024-05-01'),(8,5,950000.00,'2024-06-15'),(9,6,1600000.00,'2024-07-20'),(10,7,990000.00,'2024-08-10'),(11,8,650000.00,'2024-09-15'),(12,9,750000.00,'2024-10-01'),(13,10,220000.00,'2024-11-01'),(14,11,1800000.00,'2024-12-01'),(15,12,1000000.00,'2025-01-01');

SET FOREIGN_KEY_CHECKS = 1; 

USE fractional_ownership_db; 
SHOW TABLES; 