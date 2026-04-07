-- ============================================================
-- SUPABASE / POSTGRES VERSION OF schema_and_data.sql
-- ============================================================
-- Note:
-- 1. This uses lowercase table names for Postgres/Supabase.
-- 2. Do NOT include CREATE DATABASE / USE statements in Supabase SQL Editor.
-- 3. This file only handles schema + seed data.
-- ============================================================

drop table if exists ownership_history cascade;
drop table if exists trade cascade;
drop table if exists trade_order cascade;
drop table if exists ownership cascade;
drop table if exists wallet_transaction cascade;
drop table if exists valuation cascade;
drop table if exists ipo cascade;
drop table if exists asset cascade;
drop table if exists investor cascade;
drop table if exists admin cascade;

-- ============================================================
-- TABLE: admin
-- ============================================================
create table admin (
    admin_id integer primary key,
    admin_name varchar(100),
    email varchar(100) unique,
    role varchar(50)
);

insert into admin (admin_id, admin_name, email, role) values
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

-- ============================================================
-- TABLE: asset
-- ============================================================
create table asset (
    asset_id integer primary key,
    asset_name varchar(100),
    category varchar(50),
    description text,
    storage_location varchar(100),
    verification_reference varchar(100),
    verification_status varchar(20),
    verified_by integer references admin(admin_id)
);

create index idx_asset_category on asset(category);
create index idx_asset_verified_by on asset(verified_by);

insert into asset (
    asset_id, asset_name, category, description, storage_location,
    verification_reference, verification_status, verified_by
) values
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

-- ============================================================
-- TABLE: investor
-- ============================================================
create table investor (
    investor_id integer primary key,
    investor_name varchar(100),
    email varchar(100) unique,
    phone varchar(20),
    registration_date date,
    wallet_balance numeric(15,2) not null default 0.00
);

insert into investor (
    investor_id, investor_name, email, phone, registration_date, wallet_balance
) values
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

-- ============================================================
-- TABLE: wallet_transaction
-- ============================================================
create table wallet_transaction (
    transaction_id integer generated by default as identity primary key,
    investor_id integer not null references investor(investor_id),
    amount numeric(15,2) not null,
    transaction_type varchar(50) not null,
    transfer_category varchar(100),
    transaction_date timestamp not null default current_timestamp
);

create index idx_wallet_transaction_investor on wallet_transaction(investor_id);

insert into wallet_transaction (
    investor_id, amount, transaction_type, transfer_category, transaction_date
) values
(1, 1000.00, 'DEPOSIT',        'Bank Transfer',          '2026-03-16 20:50:36'),
(1, 1000.00, 'DEPOSIT',        'Bank Transfer',          '2026-03-16 20:51:21'),
(1,   15.50, 'DIVIDEND',       'Yield Payout',           '2026-03-16 20:51:21'),
(1, -240.00, 'ASSET_PURCHASE', 'Secondary Market Order', '2026-03-16 20:51:21');

-- ============================================================
-- TABLE: ipo
-- ============================================================
create table ipo (
    ipo_id integer primary key,
    asset_id integer unique references asset(asset_id),
    total_units integer,
    price_per_unit numeric(10,2),
    ipo_start_date date,
    ipo_end_date date,
    lock_in_period integer,
    units_sold integer not null default 0
);

insert into ipo (
    ipo_id, asset_id, total_units, price_per_unit,
    ipo_start_date, ipo_end_date, lock_in_period, units_sold
) values
(1,1,100,12000.00,'2024-01-10','2024-01-20',30,100),
(2,2,80,8500.00,'2024-02-05','2024-02-15',45,80),
(3,3,150,6000.00,'2024-03-01','2024-03-10',30,150),
(4,4,120,9500.00,'2024-04-01','2024-04-12',60,120),
(5,5,60,15000.00,'2024-05-10','2024-05-20',30,60),
(6,6,200,7000.00,'2024-06-01','2024-06-12',90,200),
(7,7,90,11000.00,'2024-07-01','2024-07-10',30,90),
(8,8,70,9000.00,'2024-08-01','2024-08-10',45,70),
(9,9,130,5000.00,'2024-09-01','2024-09-12',30,130),
(10,10,50,4000.00,'2024-10-01','2024-10-10',30,50),
(11,11,75,20000.00,'2024-11-01','2024-11-12',60,75),
(12,12,140,6500.00,'2024-12-01','2024-12-10',30,140),
(13,13,95,10500.00,'2025-01-01','2025-01-10',45,95),
(14,14,85,17000.00,'2025-02-01','2025-02-10',30,85),
(15,15,160,5500.00,'2025-03-01','2025-03-10',30,160);

-- ============================================================
-- TABLE: ownership
-- ============================================================
create table ownership (
    investor_id integer not null references investor(investor_id),
    asset_id integer not null references asset(asset_id),
    units_held integer check (units_held >= 0),
    primary key (investor_id, asset_id)
);

create index idx_ownership_asset on ownership(asset_id);

insert into ownership (investor_id, asset_id, units_held) values
(1,1,40),
(1,4,70),
(2,1,30),
(2,3,60),
(2,11,75),
(3,1,30),
(3,5,60),
(3,12,70),
(4,2,50),
(4,7,90),
(4,12,70),
(5,2,30),
(5,6,100),
(6,3,50),
(6,8,40),
(7,3,40),
(7,8,30),
(8,4,50),
(8,9,60),
(9,6,100),
(9,10,50),
(10,9,70),
(11,13,50),
(12,13,45),
(13,14,85),
(14,15,80),
(15,15,80);

-- ============================================================
-- TABLE: trade_order
-- ============================================================
create table trade_order (
    order_id integer primary key,
    investor_id integer references investor(investor_id),
    asset_id integer references asset(asset_id),
    order_type varchar(10),
    price numeric(10,2),
    units integer,
    order_date date,
    status varchar(20)
);

create index idx_trade_order_investor on trade_order(investor_id);
create index idx_trade_order_asset on trade_order(asset_id);
create index idx_trade_order_date on trade_order(order_date);

insert into trade_order (
    order_id, investor_id, asset_id, order_type, price, units, order_date, status
) values
(1,1,1,'SELL',13000.00,10,'2024-03-01','MATCHED'),
(2,4,2,'SELL',9000.00,10,'2024-04-01','MATCHED'),
(3,2,3,'BUY',6500.00,20,'2024-05-01','OPEN'),
(4,8,4,'BUY',9800.00,15,'2024-06-01','MATCHED'),
(5,6,8,'SELL',9500.00,5,'2024-09-01','OPEN'),
(6,3,12,'BUY',7000.00,10,'2025-01-15','OPEN'),
(7,11,13,'SELL',11000.00,5,'2025-02-15','OPEN'),
(8,12,13,'BUY',11500.00,5,'2025-02-16','MATCHED'),
(9,14,15,'SELL',6000.00,10,'2025-04-01','OPEN'),
(10,15,15,'BUY',6200.00,10,'2025-04-02','MATCHED'),
(11,5,6,'SELL',7200.00,15,'2024-10-01','MATCHED'),
(12,9,6,'BUY',7300.00,15,'2024-10-02','MATCHED'),
(13,7,3,'SELL',6800.00,10,'2024-09-01','OPEN'),
(14,10,9,'BUY',5200.00,20,'2024-11-01','OPEN'),
(15,8,9,'SELL',5300.00,20,'2024-11-02','MATCHED');

-- ============================================================
-- TABLE: trade
-- ============================================================
create table trade (
    trade_id integer primary key,
    trade_price numeric(10,2),
    trade_units integer,
    trade_date date,
    buy_order_id integer references trade_order(order_id),
    sell_order_id integer references trade_order(order_id)
);

create index idx_trade_buy_order on trade(buy_order_id);
create index idx_trade_sell_order on trade(sell_order_id);
create index idx_trade_date on trade(trade_date);

insert into trade (
    trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id
) values
(1,13000.00,10,'2024-03-02',1,1),
(2,9000.00,10,'2024-04-02',2,2),
(3,9800.00,15,'2024-06-02',4,4),
(4,11500.00,5,'2025-02-16',8,7),
(5,6200.00,10,'2025-04-02',10,9),
(6,7300.00,15,'2024-10-02',12,11);

-- ============================================================
-- TABLE: ownership_history
-- ============================================================
create table ownership_history (
    history_id integer generated by default as identity primary key,
    investor_id integer references investor(investor_id),
    asset_id integer references asset(asset_id),
    units_before integer,
    units_after integer,
    change_date date,
    change_type varchar(20),
    trade_id integer references trade(trade_id),
    ipo_id integer references ipo(ipo_id)
);

create index idx_ownership_history_investor on ownership_history(investor_id);
create index idx_ownership_history_asset on ownership_history(asset_id);
create index idx_ownership_history_trade on ownership_history(trade_id);
create index idx_ownership_history_ipo on ownership_history(ipo_id);

insert into ownership_history (
    history_id, investor_id, asset_id, units_before, units_after,
    change_date, change_type, trade_id, ipo_id
) values
(1,1,1,0,40,'2024-01-20','IPO',null,1),
(2,2,1,0,30,'2024-01-20','IPO',null,1),
(3,3,1,0,30,'2024-01-20','IPO',null,1),
(4,1,1,40,30,'2024-03-02','TRADE',1,null),
(5,4,2,50,40,'2024-04-02','TRADE',2,null),
(6,8,4,50,65,'2024-06-02','TRADE',3,null),
(7,11,13,50,45,'2025-02-16','TRADE',4,null),
(8,15,15,80,90,'2025-04-02','TRADE',5,null),
(9,5,6,100,85,'2024-10-02','TRADE',6,null),
(10,9,6,100,115,'2024-10-02','TRADE',6,null),
(11,13,14,0,85,'2025-02-10','IPO',null,14),
(12,14,15,0,80,'2025-03-10','IPO',null,15),
(13,15,15,0,80,'2025-03-10','IPO',null,15),
(14,11,13,0,50,'2025-01-10','IPO',null,13),
(15,12,13,0,45,'2025-01-10','IPO',null,13);

select setval(
    pg_get_serial_sequence('ownership_history', 'history_id'),
    coalesce((select max(history_id) from ownership_history), 1),
    true
);

-- ============================================================
-- TABLE: valuation
-- ============================================================
create table valuation (
    valuation_id integer primary key,
    asset_id integer references asset(asset_id),
    valuation_amount numeric(12,2),
    valuation_date date,
    unique (asset_id, valuation_date)
);

create index idx_valuation_date on valuation(valuation_date);

insert into valuation (
    valuation_id, asset_id, valuation_amount, valuation_date
) values
(1,1,1250000.00,'2024-02-01'),
(2,1,1300000.00,'2024-06-01'),
(3,2,700000.00,'2024-03-01'),
(4,2,720000.00,'2024-07-01'),
(5,3,900000.00,'2024-04-01'),
(6,3,920000.00,'2024-08-01'),
(7,4,1150000.00,'2024-05-01'),
(8,5,950000.00,'2024-06-15'),
(9,6,1600000.00,'2024-07-20'),
(10,7,990000.00,'2024-08-10'),
(11,8,650000.00,'2024-09-15'),
(12,9,750000.00,'2024-10-01'),
(13,10,220000.00,'2024-11-01'),
(14,11,1800000.00,'2024-12-01'),
(15,12,1000000.00,'2025-01-01');

-- ============================================================
-- OPTIONAL QUICK CHECKS
-- ============================================================
select 'admin' as table_name, count(*) as row_count from admin
union all
select 'asset', count(*) from asset
union all
select 'investor', count(*) from investor
union all
select 'wallet_transaction', count(*) from wallet_transaction
union all
select 'ipo', count(*) from ipo
union all
select 'ownership', count(*) from ownership
union all
select 'trade_order', count(*) from trade_order
union all
select 'trade', count(*) from trade
union all
select 'ownership_history', count(*) from ownership_history
union all
select 'valuation', count(*) from valuation
order by table_name;