--THESE ARE TEST CASES TO CHECK IF THE TRIGGERS WORK OR NOT

USE test;

--TEST 1 : Wallet deposit (after insert trigger)
-- Deposit money
INSERT INTO WALLET_TRANSACTION
(investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
(1, 500.00, 'DEPOSIT', 'Bank Transfer', NOW());

-- Check updated balance
SELECT investor_id, wallet_balance
FROM INVESTOR
WHERE investor_id = 1;

-- TEST 2 : Wallet deduction (purchase)
-- Deduct money for asset purchase
INSERT INTO WALLET_TRANSACTION
(investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
(1, -200.00, 'ASSET_PURCHASE', 'Secondary Market Order', NOW());

-- Check updated balance
SELECT investor_id, wallet_balance
FROM INVESTOR
WHERE investor_id = 1;

-- TEST 3 : Prevent negative balance (before insert trigger)
-- Try to overspend
INSERT INTO WALLET_TRANSACTION
(investor_id, amount, transaction_type, transfer_category, transaction_date)
VALUES
(1, -999999.00, 'ASSET_PURCHASE', 'Secondary Market Order', NOW());

-- TEST 4: Ownership insert logging (after insert trigger)
-- Insert ownership
INSERT INTO OWNERSHIP (investor_id, asset_id, units_held)
VALUES (1, 101, 10);
-- Check history
SELECT *
FROM OWNERSHIP_HISTORY
WHERE investor_id = 1 AND asset_id = 101;

--TEST 5 : Ownership update logging (after update trigger)
-- Update ownership
UPDATE OWNERSHIP
SET units_held = 15
WHERE investor_id = 1 AND asset_id = 101;
-- Check history again
SELECT *
FROM OWNERSHIP_HISTORY
WHERE investor_id = 1 AND asset_id = 101;

-- TEST 6 : Trade Trigger (order status update)
-- Insert trade
INSERT INTO TRADE (trade_id, buy_order_id, sell_order_id, trade_units, trade_price, trade_date)
VALUES (1001, 10, 20, 5, 200.00, NOW());
-- Check order status
SELECT order_id, status
FROM TRADE_ORDER
WHERE order_id IN (10, 20);