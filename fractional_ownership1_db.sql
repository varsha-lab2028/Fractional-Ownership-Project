-- MySQL dump 10.13  Distrib 8.0.44, for Linux (x86_64)
--
-- Host: localhost    Database: fractional_ownership_db
-- ------------------------------------------------------
-- Server version	8.0.44-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ADMIN`
--

DROP TABLE IF EXISTS `ADMIN`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ADMIN` (
  `admin_id` int NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `role` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ADMIN`
--

LOCK TABLES `ADMIN` WRITE;
/*!40000 ALTER TABLE `ADMIN` DISABLE KEYS */;
INSERT INTO `ADMIN` VALUES (1,'Ananya Rao','ananya@platform.com','Verifier'),(2,'Raghav Menon','raghav@platform.com','Verifier'),(3,'Priya Shah','priya@platform.com','Verifier'),(4,'Vikram Sethi','vikram@platform.com','Verifier'),(5,'Neha Kapoor','neha@platform.com','Verifier'),(6,'Aditya Nair','aditya@platform.com','Verifier'),(7,'Sonal Gupta','sonal@platform.com','Verifier'),(8,'Kunal Arora','kunal@platform.com','Verifier'),(9,'Ira Malhotra','ira@platform.com','Verifier'),(10,'Sameer Khan','sameer@platform.com','Verifier'),(11,'Divya Iyer','divya@platform.com','Verifier'),(12,'Manav Joshi','manav@platform.com','Verifier'),(13,'Aisha Qureshi','aisha@platform.com','Verifier'),(14,'Nikhil Bansal','nikhil@platform.com','Verifier'),(15,'Ritu Sharma','ritu@platform.com','Verifier');
/*!40000 ALTER TABLE `ADMIN` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ASSET`
--

DROP TABLE IF EXISTS `ASSET`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ASSET` (
  `asset_id` int NOT NULL,
  `name` varchar(100) DEFAULT NULL,
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ASSET`
--

LOCK TABLES `ASSET` WRITE;
/*!40000 ALTER TABLE `ASSET` DISABLE KEYS */;
INSERT INTO `ASSET` VALUES (1,'Picasso Sketch','Art','1962 pencil sketch','Vault-A1','CERT-A1','Verified',1),(2,'Rolex Daytona 1984','Watch','Vintage Rolex','Vault-B2','CERT-W2','Verified',2),(3,'Bordeaux 1990 Reserve','Wine','French wine reserve','Climate-V1','CERT-W3','Verified',3),(4,'Banksy Print','Art','Signed street art','Vault-A3','CERT-A4','Verified',4),(5,'Patek Philippe 1975','Watch','Swiss luxury watch','Vault-B4','CERT-W5','Verified',5),(6,'Macallan 1926','Wine','Rare whisky','Climate-V2','CERT-W6','Verified',6),(7,'Monet Landscape','Art','Oil painting 1880','Vault-A7','CERT-A7','Verified',7),(8,'Omega Speedmaster','Watch','Moon edition','Vault-B8','CERT-W8','Verified',8),(9,'Dom Perignon 1988','Wine','Vintage champagne','Climate-V3','CERT-W9','Verified',9),(10,'Van Gogh Replica','Art','Museum replica','Vault-A10','CERT-A10','Verified',10),(11,'Richard Mille RM011','Watch','Luxury sports watch','Vault-B11','CERT-W11','Verified',11),(12,'Screaming Eagle 1992','Wine','Rare California wine','Climate-V4','CERT-W12','Verified',12),(13,'Andy Warhol Poster','Art','Pop art original','Vault-A13','CERT-A13','Verified',13),(14,'Audemars Piguet Royal Oak','Watch','Steel sports watch','Vault-B14','CERT-W14','Verified',14),(15,'Chateau Lafite 2000','Wine','French grand cru','Climate-V5','CERT-W15','Verified',15);
/*!40000 ALTER TABLE `ASSET` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `INVESTOR`
--

DROP TABLE IF EXISTS `INVESTOR`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `INVESTOR` (
  `investor_id` int NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `registration_date` date DEFAULT NULL,
  PRIMARY KEY (`investor_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `INVESTOR`
--

LOCK TABLES `INVESTOR` WRITE;
/*!40000 ALTER TABLE `INVESTOR` DISABLE KEYS */;
INSERT INTO `INVESTOR` VALUES (1,'Aman Gupta','aman@gmail.com','2023-11-12'),(2,'Riya Malhotra','riya@gmail.com','2023-12-01'),(3,'Karan Mehta','karan@gmail.com','2024-01-15'),(4,'Sneha Iyer','sneha@gmail.com','2024-02-03'),(5,'Arjun Verma','arjun@gmail.com','2024-03-20'),(6,'Meera Jain','meera@gmail.com','2024-04-05'),(7,'Rahul Bose','rahul@gmail.com','2024-05-11'),(8,'Tanya Roy','tanya@gmail.com','2024-06-18'),(9,'Dev Khanna','dev@gmail.com','2024-07-02'),(10,'Ishita Singh','ishita@gmail.com','2024-08-10'),(11,'Harsh Patel','harsh@gmail.com','2024-09-01'),(12,'Neeraj Sood','neeraj@gmail.com','2024-09-20'),(13,'Simran Kaur','simran@gmail.com','2024-10-05'),(14,'Rohit Das','rohit@gmail.com','2024-11-12'),(15,'Pooja Nair','pooja@gmail.com','2024-12-01');
/*!40000 ALTER TABLE `INVESTOR` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `IPO`
--

DROP TABLE IF EXISTS `IPO`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `IPO` (
  `ipo_id` int NOT NULL,
  `asset_id` int DEFAULT NULL,
  `total_units` int DEFAULT NULL,
  `price_per_unit` decimal(10,2) DEFAULT NULL,
  `ipo_start_date` date DEFAULT NULL,
  `ipo_end_date` date DEFAULT NULL,
  `lock_in_period` int DEFAULT NULL,
  PRIMARY KEY (`ipo_id`),
  UNIQUE KEY `asset_id` (`asset_id`),
  CONSTRAINT `IPO_ibfk_1` FOREIGN KEY (`asset_id`) REFERENCES `ASSET` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `IPO`
--

LOCK TABLES `IPO` WRITE;
/*!40000 ALTER TABLE `IPO` DISABLE KEYS */;
INSERT INTO `IPO` VALUES (1,1,100,12000.00,'2024-01-10','2024-01-20',30),(2,2,80,8500.00,'2024-02-05','2024-02-15',45),(3,3,150,6000.00,'2024-03-01','2024-03-10',30),(4,4,120,9500.00,'2024-04-01','2024-04-12',60),(5,5,60,15000.00,'2024-05-10','2024-05-20',30),(6,6,200,7000.00,'2024-06-01','2024-06-12',90),(7,7,90,11000.00,'2024-07-01','2024-07-10',30),(8,8,70,9000.00,'2024-08-01','2024-08-10',45),(9,9,130,5000.00,'2024-09-01','2024-09-12',30),(10,10,50,4000.00,'2024-10-01','2024-10-10',30),(11,11,75,20000.00,'2024-11-01','2024-11-12',60),(12,12,140,6500.00,'2024-12-01','2024-12-10',30),(13,13,95,10500.00,'2025-01-01','2025-01-10',45),(14,14,85,17000.00,'2025-02-01','2025-02-10',30),(15,15,160,5500.00,'2025-03-01','2025-03-10',30);
/*!40000 ALTER TABLE `IPO` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `OWNERSHIP`
--

DROP TABLE IF EXISTS `OWNERSHIP`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `OWNERSHIP`
--

LOCK TABLES `OWNERSHIP` WRITE;
/*!40000 ALTER TABLE `OWNERSHIP` DISABLE KEYS */;
INSERT INTO `OWNERSHIP` VALUES (1,1,40),(1,4,70),(2,1,30),(2,3,60),(2,11,75),(3,1,30),(3,5,60),(3,12,70),(4,2,50),(4,7,90),(4,12,70),(5,2,30),(5,6,100),(6,3,50),(6,8,40),(7,3,40),(7,8,30),(8,4,50),(8,9,60),(9,6,100),(9,10,50),(10,9,70),(11,13,50),(12,13,45),(13,14,85),(14,15,80),(15,15,80);
/*!40000 ALTER TABLE `OWNERSHIP` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `OWNERSHIP_HISTORY`
--

DROP TABLE IF EXISTS `OWNERSHIP_HISTORY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `OWNERSHIP_HISTORY` (
  `history_id` int NOT NULL,
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `OWNERSHIP_HISTORY`
--

LOCK TABLES `OWNERSHIP_HISTORY` WRITE;
/*!40000 ALTER TABLE `OWNERSHIP_HISTORY` DISABLE KEYS */;
INSERT INTO `OWNERSHIP_HISTORY` VALUES (1,1,1,0,40,'2024-01-20','IPO',NULL,1),(2,2,1,0,30,'2024-01-20','IPO',NULL,1),(3,3,1,0,30,'2024-01-20','IPO',NULL,1),(4,1,1,40,30,'2024-03-02','TRADE',1,NULL),(5,4,2,50,40,'2024-04-02','TRADE',2,NULL),(6,8,4,50,65,'2024-06-02','TRADE',3,NULL),(7,11,13,50,45,'2025-02-16','TRADE',4,NULL),(8,15,15,80,90,'2025-04-02','TRADE',5,NULL),(9,5,6,100,85,'2024-10-02','TRADE',6,NULL),(10,9,6,100,115,'2024-10-02','TRADE',6,NULL),(11,13,14,0,85,'2025-02-10','IPO',NULL,14),(12,14,15,0,80,'2025-03-10','IPO',NULL,15),(13,15,15,0,80,'2025-03-10','IPO',NULL,15),(14,11,13,0,50,'2025-01-10','IPO',NULL,13),(15,12,13,0,45,'2025-01-10','IPO',NULL,13);
/*!40000 ALTER TABLE `OWNERSHIP_HISTORY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TRADE`
--

DROP TABLE IF EXISTS `TRADE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TRADE`
--

LOCK TABLES `TRADE` WRITE;
/*!40000 ALTER TABLE `TRADE` DISABLE KEYS */;
INSERT INTO `TRADE` VALUES (1,13000.00,10,'2024-03-02',1,1),(2,9000.00,10,'2024-04-02',2,2),(3,9800.00,15,'2024-06-02',4,4),(4,11500.00,5,'2025-02-16',8,7),(5,6200.00,10,'2025-04-02',10,9),(6,7300.00,15,'2024-10-02',12,11);
/*!40000 ALTER TABLE `TRADE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TRADE_ORDER`
--

DROP TABLE IF EXISTS `TRADE_ORDER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TRADE_ORDER`
--

LOCK TABLES `TRADE_ORDER` WRITE;
/*!40000 ALTER TABLE `TRADE_ORDER` DISABLE KEYS */;
INSERT INTO `TRADE_ORDER` VALUES (1,1,1,'SELL',13000.00,10,'2024-03-01','MATCHED'),(2,4,2,'SELL',9000.00,10,'2024-04-01','MATCHED'),(3,2,3,'BUY',6500.00,20,'2024-05-01','OPEN'),(4,8,4,'BUY',9800.00,15,'2024-06-01','MATCHED'),(5,6,8,'SELL',9500.00,5,'2024-09-01','OPEN'),(6,3,12,'BUY',7000.00,10,'2025-01-15','OPEN'),(7,11,13,'SELL',11000.00,5,'2025-02-15','OPEN'),(8,12,13,'BUY',11500.00,5,'2025-02-16','MATCHED'),(9,14,15,'SELL',6000.00,10,'2025-04-01','OPEN'),(10,15,15,'BUY',6200.00,10,'2025-04-02','MATCHED'),(11,5,6,'SELL',7200.00,15,'2024-10-01','MATCHED'),(12,9,6,'BUY',7300.00,15,'2024-10-02','MATCHED'),(13,7,3,'SELL',6800.00,10,'2024-09-01','OPEN'),(14,10,9,'BUY',5200.00,20,'2024-11-01','OPEN'),(15,8,9,'SELL',5300.00,20,'2024-11-02','MATCHED');
/*!40000 ALTER TABLE `TRADE_ORDER` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `VALUATION`
--

DROP TABLE IF EXISTS `VALUATION`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `VALUATION`
--

LOCK TABLES `VALUATION` WRITE;
/*!40000 ALTER TABLE `VALUATION` DISABLE KEYS */;
INSERT INTO `VALUATION` VALUES (1,1,1250000.00,'2024-02-01'),(2,1,1300000.00,'2024-06-01'),(3,2,700000.00,'2024-03-01'),(4,2,720000.00,'2024-07-01'),(5,3,900000.00,'2024-04-01'),(6,3,920000.00,'2024-08-01'),(7,4,1150000.00,'2024-05-01'),(8,5,950000.00,'2024-06-15'),(9,6,1600000.00,'2024-07-20'),(10,7,990000.00,'2024-08-10'),(11,8,650000.00,'2024-09-15'),(12,9,750000.00,'2024-10-01'),(13,10,220000.00,'2024-11-01'),(14,11,1800000.00,'2024-12-01'),(15,12,1000000.00,'2025-01-01');
/*!40000 ALTER TABLE `VALUATION` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-02 16:39:37
