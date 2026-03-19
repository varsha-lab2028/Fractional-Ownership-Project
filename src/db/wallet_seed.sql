USE test;


-- ============================================================
-- WALLET SEED DATA  —  test
-- ============================================================
-- Inserts WALLET_TRANSACTION rows for all 15 investors and
-- then explicitly UPDATEs INVESTOR.wallet_balance for every
-- investor so the table is fully in sync.
-- ============================================================

USE test;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- STEP 1 — Clean slate
-- ============================================================
DELETE FROM WALLET_TRANSACTION;
UPDATE INVESTOR SET wallet_balance = 0.00;

-- ============================================================
-- STEP 2 — INVESTOR 1  Aman Gupta
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (1,  800000.00, 'DEPOSIT',        'Bank Transfer',           '2024-01-05 09:00:00'),
    (1,  800000.00, 'DEPOSIT',        'Bank Transfer',           '2024-01-07 14:30:00'),
    (1,  400000.00, 'DEPOSIT',        'UPI Transfer',            '2024-01-09 11:15:00'),
    (1, -480000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 1',  '2024-01-10 10:00:00'),
    (1, -665000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 4',  '2024-04-01 10:00:00'),
    (1,  130000.00, 'ASSET_SALE',     'Trade Execution',         '2024-03-02 15:45:00'),
    (1,    1950.00, 'DIVIDEND',       'Yield Payout - Asset 1',  '2024-06-30 08:00:00'),
    (1,    3354.17, 'DIVIDEND',       'Yield Payout - Asset 4',  '2024-09-30 08:00:00'),
    (1,   50000.00, 'DEPOSIT',        'Bank Transfer',           '2024-11-01 10:00:00'),
    (1,  -80000.00, 'WITHDRAWAL',     'Bank Transfer',           '2024-12-15 16:00:00'),
    (1,    1950.00, 'DIVIDEND',       'Yield Payout - Asset 1',  '2024-12-31 08:00:00'),
    (1,    5000.00, 'DIVIDEND',       'Yield Payout - Asset 4',  '2025-03-31 08:00:00'),
    (1,  100000.00, 'DEPOSIT',        'Bank Transfer',           '2025-04-01 09:00:00');

UPDATE INVESTOR SET wallet_balance = 1067254.17 WHERE investor_id = 1;

-- ============================================================
-- STEP 3 — INVESTOR 2  Riya Malhotra
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (2, 2000000.00, 'DEPOSIT',        'Wire Transfer',            '2023-12-15 09:00:00'),
    (2,  500000.00, 'DEPOSIT',        'Bank Transfer',            '2024-01-08 10:30:00'),
    (2, -360000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 1',   '2024-01-10 10:05:00'),
    (2, -360000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 3',   '2024-03-01 10:05:00'),
    (2,-1500000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 11',  '2024-11-01 10:05:00'),
    (2,    1950.00, 'DIVIDEND',       'Yield Payout - Asset 1',   '2024-09-30 08:00:00'),
    (2,    9000.00, 'DIVIDEND',       'Yield Payout - Asset 11',  '2024-12-31 08:00:00'),
    (2,    2000.00, 'DIVIDEND',       'Yield Payout - Asset 3',   '2024-12-31 08:05:00'),
    (2,  150000.00, 'DEPOSIT',        'Bank Transfer',            '2025-04-01 09:00:00'),
    (2,    9000.00, 'DIVIDEND',       'Yield Payout - Asset 11',  '2025-03-31 08:00:00');

UPDATE INVESTOR SET wallet_balance = 451950.00 WHERE investor_id = 2;

-- ============================================================
-- STEP 4 — INVESTOR 3  Karan Mehta
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (3, 1000000.00, 'DEPOSIT',        'Bank Transfer',            '2024-01-08 11:00:00'),
    (3, 1000000.00, 'DEPOSIT',        'Bank Transfer',            '2024-01-09 11:00:00'),
    (3,  500000.00, 'DEPOSIT',        'Wire Transfer',            '2024-05-01 11:00:00'),
    (3, -360000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 1',   '2024-01-10 10:10:00'),
    (3, -900000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 5',   '2024-05-10 10:10:00'),
    (3, -455000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 12',  '2024-12-01 10:10:00'),
    (3,    4750.00, 'DIVIDEND',       'Yield Payout - Asset 5',   '2024-09-30 08:00:00'),
    (3,    2500.00, 'DIVIDEND',       'Yield Payout - Asset 12',  '2024-12-31 08:00:00'),
    (3,    1950.00, 'DIVIDEND',       'Yield Payout - Asset 1',   '2024-12-31 08:05:00'),
    (3,    4750.00, 'DIVIDEND',       'Yield Payout - Asset 5',   '2025-03-31 08:00:00'),
    (3,    2500.00, 'DIVIDEND',       'Yield Payout - Asset 12',  '2025-03-31 08:05:00');

UPDATE INVESTOR SET wallet_balance = 801450.00 WHERE investor_id = 3;

-- ============================================================
-- STEP 5 — INVESTOR 4  Sneha Iyer
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (4,  600000.00, 'DEPOSIT',        'Bank Transfer',            '2024-02-01 09:00:00'),
    (4,  600000.00, 'DEPOSIT',        'Bank Transfer',            '2024-02-03 09:00:00'),
    (4,  600000.00, 'DEPOSIT',        'Wire Transfer',            '2024-06-01 09:00:00'),
    (4,  600000.00, 'DEPOSIT',        'Bank Transfer',            '2024-07-01 09:00:00'),
    (4, -425000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 2',   '2024-02-05 10:15:00'),
    (4,   90000.00, 'ASSET_SALE',     'Trade Execution',          '2024-04-02 14:00:00'),
    (4, -990000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 7',   '2024-07-01 10:15:00'),
    (4, -455000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 12',  '2024-12-01 10:15:00'),
    (4,    1800.00, 'DIVIDEND',       'Yield Payout - Asset 2',   '2024-09-30 08:00:00'),
    (4,    4950.00, 'DIVIDEND',       'Yield Payout - Asset 7',   '2024-12-31 08:00:00'),
    (4,    2500.00, 'DIVIDEND',       'Yield Payout - Asset 12',  '2024-12-31 08:05:00'),
    (4,    4950.00, 'DIVIDEND',       'Yield Payout - Asset 7',   '2025-03-31 08:00:00');

UPDATE INVESTOR SET wallet_balance = 634200.00 WHERE investor_id = 4;

-- ============================================================
-- STEP 6 — INVESTOR 5  Arjun Verma
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (5,  400000.00, 'DEPOSIT',        'Bank Transfer',            '2024-02-03 10:00:00'),
    (5,  600000.00, 'DEPOSIT',        'UPI Transfer',             '2024-02-04 10:00:00'),
    (5, -255000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 2',   '2024-02-05 10:20:00'),
    (5, -700000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 6',   '2024-06-01 10:20:00'),
    (5,  109500.00, 'ASSET_SALE',     'Trade Execution',          '2024-10-02 14:00:00'),
    (5,    3400.00, 'DIVIDEND',       'Yield Payout - Asset 6',   '2024-12-31 08:00:00'),
    (5,    3400.00, 'DIVIDEND',       'Yield Payout - Asset 6',   '2025-03-31 08:00:00'),
    (5,   50000.00, 'DEPOSIT',        'Bank Transfer',            '2025-01-15 09:00:00');

UPDATE INVESTOR SET wallet_balance = 211300.00 WHERE investor_id = 5;

-- ============================================================
-- STEP 7 — INVESTOR 6  Meera Jain
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (6,  400000.00, 'DEPOSIT',        'Bank Transfer',            '2024-02-28 09:00:00'),
    (6,  400000.00, 'DEPOSIT',        'Bank Transfer',            '2024-08-01 09:00:00'),
    (6, -300000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 3',   '2024-03-01 10:25:00'),
    (6, -360000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 8',   '2024-08-01 10:25:00'),
    (6,    1533.33, 'DIVIDEND',       'Yield Payout - Asset 3',   '2024-09-30 08:00:00'),
    (6,    1857.14, 'DIVIDEND',       'Yield Payout - Asset 8',   '2024-12-31 08:00:00'),
    (6,    1533.33, 'DIVIDEND',       'Yield Payout - Asset 3',   '2025-03-31 08:00:00'),
    (6,    1857.14, 'DIVIDEND',       'Yield Payout - Asset 8',   '2025-03-31 08:05:00'),
    (6,   50000.00, 'DEPOSIT',        'Bank Transfer',            '2025-02-01 09:00:00');

UPDATE INVESTOR SET wallet_balance = 196780.94 WHERE investor_id = 6;

-- ============================================================
-- STEP 8 — INVESTOR 7  Rahul Bose
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (7,  300000.00, 'DEPOSIT',        'Bank Transfer',            '2024-02-28 10:00:00'),
    (7,  400000.00, 'DEPOSIT',        'UPI Transfer',             '2024-08-01 10:00:00'),
    (7, -240000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 3',   '2024-03-01 10:30:00'),
    (7, -270000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 8',   '2024-08-01 10:30:00'),
    (7,    1226.67, 'DIVIDEND',       'Yield Payout - Asset 3',   '2024-12-31 08:00:00'),
    (7,    1400.00, 'DIVIDEND',       'Yield Payout - Asset 8',   '2024-12-31 08:05:00'),
    (7,    1226.67, 'DIVIDEND',       'Yield Payout - Asset 3',   '2025-03-31 08:00:00'),
    (7,    1400.00, 'DIVIDEND',       'Yield Payout - Asset 8',   '2025-03-31 08:05:00'),
    (7,   50000.00, 'DEPOSIT',        'Bank Transfer',            '2025-01-10 09:00:00');

UPDATE INVESTOR SET wallet_balance = 245253.34 WHERE investor_id = 7;

-- ============================================================
-- STEP 9 — INVESTOR 8  Tanya Roy
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (8,  600000.00, 'DEPOSIT',        'Bank Transfer',            '2024-03-28 09:00:00'),
    (8,  400000.00, 'DEPOSIT',        'Wire Transfer',            '2024-08-30 09:00:00'),
    (8, -475000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 4',   '2024-04-01 10:35:00'),
    (8, -147000.00, 'ASSET_PURCHASE', 'Trade Execution',          '2024-06-02 14:00:00'),
    (8, -300000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 9',   '2024-09-01 10:35:00'),
    (8,    3114.58, 'DIVIDEND',       'Yield Payout - Asset 4',   '2024-12-31 08:00:00'),
    (8,    2884.62, 'DIVIDEND',       'Yield Payout - Asset 9',   '2024-12-31 08:05:00'),
    (8,    3114.58, 'DIVIDEND',       'Yield Payout - Asset 4',   '2025-03-31 08:00:00'),
    (8,   50000.00, 'DEPOSIT',        'Bank Transfer',            '2025-02-01 09:00:00');

UPDATE INVESTOR SET wallet_balance = 137113.78 WHERE investor_id = 8;

-- ============================================================
-- STEP 10 — INVESTOR 9  Dev Khanna
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (9,  800000.00, 'DEPOSIT',        'Bank Transfer',            '2024-06-01 09:30:00'),
    (9,  500000.00, 'DEPOSIT',        'Bank Transfer',            '2024-09-28 09:30:00'),
    (9, -700000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 6',   '2024-06-01 10:40:00'),
    (9, -109500.00, 'ASSET_PURCHASE', 'Trade Execution',          '2024-10-02 14:00:00'),
    (9, -200000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 10',  '2024-10-01 10:40:00'),
    (9,    4600.00, 'DIVIDEND',       'Yield Payout - Asset 6',   '2024-12-31 08:00:00'),
    (9,    2200.00, 'DIVIDEND',       'Yield Payout - Asset 10',  '2024-12-31 08:05:00'),
    (9,  200000.00, 'DEPOSIT',        'Bank Transfer',            '2025-01-01 09:00:00'),
    (9,    4600.00, 'DIVIDEND',       'Yield Payout - Asset 6',   '2025-03-31 08:00:00');

UPDATE INVESTOR SET wallet_balance = 501900.00 WHERE investor_id = 9;

-- ============================================================
-- STEP 11 — INVESTOR 10  Ishita Singh
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (10,  450000.00, 'DEPOSIT',        'Bank Transfer',           '2024-08-30 09:00:00'),
    (10, -350000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 9',  '2024-09-01 10:45:00'),
    (10,    2019.23, 'DIVIDEND',       'Yield Payout - Asset 9',  '2024-12-31 08:00:00'),
    (10,  110000.00, 'DEPOSIT',        'UPI Transfer',            '2025-01-01 09:00:00'),
    (10,    2019.23, 'DIVIDEND',       'Yield Payout - Asset 9',  '2025-03-31 08:00:00'),
    (10,   50000.00, 'DEPOSIT',        'Bank Transfer',           '2025-04-01 09:00:00');

UPDATE INVESTOR SET wallet_balance = 264038.46 WHERE investor_id = 10;

-- ============================================================
-- STEP 12 — INVESTOR 11  Harsh Patel
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (11,  600000.00, 'DEPOSIT',        'Bank Transfer',           '2024-12-28 09:00:00'),
    (11, -525000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 13', '2025-01-01 10:50:00'),
    (11,   57500.00, 'ASSET_SALE',     'Trade Execution',         '2025-02-16 14:00:00'),
    (11,    2362.50, 'DIVIDEND',       'Yield Payout - Asset 13', '2025-03-31 08:00:00'),
    (11,   50000.00, 'DEPOSIT',        'Bank Transfer',           '2025-04-01 09:00:00');

UPDATE INVESTOR SET wallet_balance = 184862.50 WHERE investor_id = 11;

-- ============================================================
-- STEP 13 — INVESTOR 12  Neeraj Sood
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (12,  600000.00, 'DEPOSIT',        'Bank Transfer',           '2024-12-28 09:30:00'),
    (12, -472500.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 13', '2025-01-01 10:55:00'),
    (12,  -57500.00, 'ASSET_PURCHASE', 'Trade Execution',         '2025-02-16 14:00:00'),
    (12,    2625.00, 'DIVIDEND',       'Yield Payout - Asset 13', '2025-03-31 08:00:00'),
    (12,   50000.00, 'DEPOSIT',        'Bank Transfer',           '2025-04-01 09:00:00');

UPDATE INVESTOR SET wallet_balance = 122625.00 WHERE investor_id = 12;

-- ============================================================
-- STEP 14 — INVESTOR 13  Simran Kaur
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (13, 1500000.00, 'DEPOSIT',        'Wire Transfer',           '2025-01-28 09:00:00'),
    (13,-1445000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 14', '2025-02-01 11:00:00'),
    (13,    7225.00, 'DIVIDEND',       'Yield Payout - Asset 14', '2025-03-31 08:00:00'),
    (13,   50000.00, 'DEPOSIT',        'Bank Transfer',           '2025-03-01 09:00:00'),
    (13,    7225.00, 'DIVIDEND',       'Yield Payout - Asset 14', '2025-06-30 08:00:00');

UPDATE INVESTOR SET wallet_balance = 119450.00 WHERE investor_id = 13;

-- ============================================================
-- STEP 15 — INVESTOR 14  Rohit Das
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (14,  500000.00, 'DEPOSIT',        'Bank Transfer',           '2025-02-28 09:00:00'),
    (14, -440000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 15', '2025-03-01 11:05:00'),
    (14,   62000.00, 'ASSET_SALE',     'Trade Execution',         '2025-04-02 14:00:00'),
    (14,  -30000.00, 'WITHDRAWAL',     'Bank Transfer',           '2025-04-10 10:00:00'),
    (14,    2200.00, 'DIVIDEND',       'Yield Payout - Asset 15', '2025-03-31 08:00:00'),
    (14,   50000.00, 'DEPOSIT',        'Bank Transfer',           '2025-04-15 09:00:00');

UPDATE INVESTOR SET wallet_balance = 144200.00 WHERE investor_id = 14;

-- ============================================================
-- STEP 16 — INVESTOR 15  Pooja Nair
-- ============================================================
INSERT INTO WALLET_TRANSACTION
    (investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
    (15,  500000.00, 'DEPOSIT',        'Bank Transfer',           '2025-02-28 10:00:00'),
    (15, -440000.00, 'ASSET_PURCHASE', 'IPO Purchase - Asset 15', '2025-03-01 11:10:00'),
    (15,  -62000.00, 'ASSET_PURCHASE', 'Trade Execution',         '2025-04-02 14:00:00'),
    (15,    2475.00, 'DIVIDEND',       'Yield Payout - Asset 15', '2025-03-31 08:00:00'),
    (15,   50000.00, 'DEPOSIT',        'Bank Transfer',           '2025-04-15 09:00:00'),
    (15,    2475.00, 'DIVIDEND',       'Yield Payout - Asset 15', '2025-06-30 08:00:00');

UPDATE INVESTOR SET wallet_balance = 52950.00 WHERE investor_id = 15;

-- ============================================================
-- STEP 17 — SAFETY NET
-- Recalculate every balance from the transaction log in case
-- any floating-point rounding caused a 1-cent drift.
-- This is the authoritative final UPDATE.
-- ============================================================
UPDATE INVESTOR i
JOIN (
    SELECT investor_id, ROUND(SUM(amount), 2) AS computed_balance
    FROM WALLET_TRANSACTION
    GROUP BY investor_id
) totals ON totals.investor_id = i.investor_id
SET i.wallet_balance = totals.computed_balance;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- VERIFICATION — run these immediately after to confirm
-- ============================================================

-- Check 1: stored balance matches computed balance for every investor
SELECT
    i.investor_id,
    i.wallet_balance                                         AS stored_balance,
    ROUND(COALESCE(SUM(wt.amount), 0), 2)                    AS computed_balance,
    ROUND(i.wallet_balance - COALESCE(SUM(wt.amount), 0), 2) AS drift
FROM INVESTOR i
LEFT JOIN WALLET_TRANSACTION wt ON wt.investor_id = i.investor_id
GROUP BY i.investor_id, i.wallet_balance
ORDER BY i.investor_id;

-- Check 2: transaction counts and type breakdown per investor
SELECT
    i.investor_id,
    wt.transaction_type,
    COUNT(*)        AS txn_count,
    SUM(wt.amount)  AS net_amount
FROM WALLET_TRANSACTION wt
JOIN INVESTOR i ON i.investor_id = wt.investor_id
GROUP BY i.investor_id, wt.transaction_type
ORDER BY i.investor_id, wt.transaction_type;

-- Check 3: no investor should have a zero or negative balance
SELECT investor_id, wallet_balance
FROM INVESTOR
WHERE wallet_balance <= 0.00
ORDER BY investor_id;

-- Check 4: total row counts
SELECT
    COUNT(*)                                    AS total_transactions,
    COUNT(DISTINCT investor_id)                 AS investors_with_transactions,
    SUM(CASE WHEN amount > 0 THEN amount END)   AS total_credits,
    SUM(CASE WHEN amount < 0 THEN ABS(amount) END) AS total_debits
FROM WALLET_TRANSACTION;