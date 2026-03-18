# 📊 Fractional Ownership Marketplace for Passion Assets

## 🧠 Project Overview

This project is a **DBMS-based marketplace simulation** that enables users to invest in **fractional ownership of high-value passion assets** such as collectibles, luxury items, and rare objects.

Instead of purchasing an entire asset, investors can buy **units (fractions)** of an asset, similar to shares in financial markets.

The system demonstrates how database systems can support:

* asset listings and IPO-like offerings
* ownership tracking
* transaction handling
* portfolio management
* wallet systems
* admin control

⚠️ This is an **academic project** built using **Java (Swing) + MySQL** with **seeded data**, not a real-time trading system.

---

## 🏗️ Tech Stack

* **Language:** Java
* **UI Framework:** Java Swing
* **Database:** MySQL
* **Build Tool:** Maven
* **Architecture:** Layered (UI → Service → DAO → Database)

---

## 🧩 System Architecture

The project follows a layered architecture:

```
UI Layer (Swing)
    ↓
Service Layer (Business Logic)
    ↓
DAO Layer (SQL Operations)
    ↓
MySQL Database
```

### Components

* **UI Layer:** Handles user interaction (Investor/Admin dashboards)
* **Service Layer:** Implements business logic and workflows
* **DAO Layer:** Executes SQL queries and database operations
* **DTO Layer:** Transfers structured data to UI
* **Domain Layer:** Represents entities like Investor, Asset, IPO, etc.

---

## 🗃️ Database Schema

The system is built around the following tables:

| Table               | Description                            |
| ------------------- | -------------------------------------- |
| `ADMIN`             | Admin users managing the system        |
| `ASSET`             | Asset metadata and verification status |
| `INVESTOR`          | Investor profiles                      |
| `IPO`               | Initial offering of asset units        |
| `OWNERSHIP`         | Current ownership records              |
| `TRADE_ORDER`       | Buy/Sell orders                        |
| `TRADE`             | Executed trades                        |
| `OWNERSHIP_HISTORY` | Ownership change history               |
| `VALUATION`         | Asset valuation data                   |

---

## 👥 User Roles

### 👤 Investor

Investors can:

* View available assets and IPOs
* Buy fractional units of assets
* View portfolio and holdings
* Track wallet balance and transactions
* Analyze dashboard insights

---

### 🛠️ Admin

Admins can:

* Manage assets
* Verify listings
* Launch IPOs
* Monitor investor activity

---

## 📱 Features

### ✅ Implemented

* Authentication & login system
* DAO-based database access
* Multiple UI screens (Investor/Admin)
* Asset & IPO listings
* SQL query implementation
* Portfolio & dashboard UI structure
* Wallet and holdings UI

---

### 🚧 Partially Implemented

* Market filtering, search, and sorting
* Portfolio valuation calculations
* Wallet transaction logic
* Dashboard analytics

---

### ❌ Planned / Not Fully Implemented

* Trade execution workflow
* Transaction management (atomic operations)
* Stored procedures
* Triggers
* SQL functions
* Conflict simulation (concurrency handling)

---

## 🔁 Core Workflow

1. Admin adds and verifies assets
2. Admin launches IPO for an asset
3. Investors browse market listings
4. Investors buy asset units
5. System updates:

   * wallet balance
   * ownership records
   * trade history
   * ownership history
6. Dashboard reflects updated portfolio

---

## 🧪 Advanced DBMS Concepts

This project incorporates:

* Complex SQL queries (joins, aggregations)
* Transactions for data consistency
* Stored procedures (planned)
* Triggers (planned)
* SQL functions (planned)
* JDBC-based embedded SQL

---

## 📂 Project Resources

All supporting materials for this project are available here:

🔗 [Access Project Drive](https://drive.google.com/drive/u/2/folders/1H7biLlWui_KqkorA180rrt3pmEbZZq3p)

Contents include:

* ER Diagram
* Project documentation
* Design resources
* Additional supporting files

---

## ⚙️ Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/<your-username>/Fractional-Ownership-Project.git
cd Fractional-Ownership-Project
```

---

### 2. Setup MySQL Database

Create database:

```sql
CREATE DATABASE fractional_ownership_db;
```

Import schema and seed data from SQL files.

---

### 3. Configure Database Connection

Update DB credentials in:

```
ServerConnector.java / DBConnection.java
```

Example:

```java
url = "jdbc:mysql://localhost:3306/fractional_ownership_db";
username = "root";
password = "your_password";
```

---

### 4. Build the Project

```bash
mvn clean compile
```

---

### 5. Run the Application

Run:

```
loginUI.java
```

---

## ⚠️ Important Notes

* Uses **seeded data**, not real-time APIs
* Designed for **academic demonstration purposes**
* Some advanced features are still under development
* Build artifacts (`target/`, `.class`) are excluded via `.gitignore`

---

## 🚀 Future Improvements

* Complete trade execution engine
* Implement stored procedures and triggers
* Add transaction conflict simulation
* Improve dashboard analytics
* Enhance UI responsiveness

---

## 🤝 Contributors

* Varshamegana Atmakuri
* Disha Kukkal
* Satwik Biswas

---

## 📚 Academic Context

This project is part of a **Database Management Systems (DBMS)** course and demonstrates:

* relational schema design
* SQL proficiency
* backend integration
* transaction handling
* advanced SQL usage

---

## ⭐ Final Note

This project simulates how real-world systems manage:

* fractional ownership
* investment flows
* portfolio tracking

within the scope of a structured DBMS implementation.
