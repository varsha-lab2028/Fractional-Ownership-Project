USE fractional_ownership_db;

--15 SQL queries
--Q1 
SELECT asset_id, asset_name, category, storage_location, verification_status FROM ASSET WHERE verification_status = 'Verified';

--Q2 
SELECT a.asset_id, a.asset_name, a.verification_status, ad.admin_id, ad.admin_name FROM ASSET a LEFT JOIN ADMIN ad ON ad.admin_id = a.verified_by;

--Q3
SELECT i.ipo_id, a.asset_name, i.total_units, i.price_per_unit FROM IPO i JOIN ASSET a ON a.asset_id = i.asset_id;

--Q4 For finding IPOs active on today's date (Active IPOs)
SELECT * FROM IPO WHERE CURDATE() BETWEEN ipo_start_date AND ipo_end_date;

--Q5 Listing IPOs for seeded data by using a reference date (used for the project)
SELECT i.ipo_id,
       a.asset_id,
       a.asset_name,
       a.category,
       i.total_units,
       i.price_per_unit,
       i.ipo_start_date,
       i.ipo_end_date,
       i.lock_in_period
FROM IPO i JOIN ASSET a ON a.asset_id = i.asset_id
WHERE DATE('2025-02-05') BETWEEN DATE(i.ipo_start_date) AND DATE(i.ipo_end_date);

--Q6
SELECT o.investor_id, inv.investor_name, a.asset_name, o.units_held FROM OWNERSHIP o JOIN INVESTOR inv ON inv.investor_id = o.investor_id JOIN ASSET a ON a.asset_id = o.asset_id;

--Q7
SELECT asset_id, SUM(units_held) AS total_units FROM OWNERSHIP GROUP BY asset_id;

--Q8
SELECT investor_id, SUM(units_held) AS total_units FROM OWNERSHIP GROUP BY investor_id ORDER BY total_units DESC;

--Q9
SELECT a.asset_id, a.asset_name, v.valuation_amount FROM ASSET a LEFT JOIN VALUATION v ON a.asset_id = v.asset_id;

--Q10
SELECT asset_id FROM ASSET a WHERE NOT EXISTS (SELECT 1 FROM TRADE_ORDER o WHERE o.asset_id = a.asset_id);

--Q11
SELECT investor_id FROM INVESTOR i WHERE EXISTS (SELECT 1 FROM TRADE_ORDER o WHERE o.investor_id = i.investor_id);

--Q12
SELECT investor_id FROM OWNERSHIP UNION SELECT investor_id FROM TRADE_ORDER;

--Q13
SELECT i.asset_id, i.total_units, SUM(o.units_held) AS total_held FROM IPO i LEFT JOIN OWNERSHIP o ON o.asset_id = i.asset_id GROUP BY i.asset_id, i.total_units HAVING SUM(o.units_held) > i.total_units;

--Q14
SELECT investor_id FROM OWNERSHIP WHERE investor_id NOT IN (SELECT investor_id FROM TRADE_ORDER);

--Q15 Show latest valuation of each asset
SELECT v.asset_id, a.asset_name, v.valuation_amount, v.valuation_date FROM VALUATION v JOIN ASSET a ON v.asset_id = a.asset_id
WHERE (v.asset_id, v.valuation_date) IN (SELECT asset_id, MAX(valuation_date) FROM VALUATION GROUP BY asset_id);

--Q16 Show matched trades with buyer and seller investors
SELECT t.trade_id,
       t.trade_date,
       t.trade_price,
       t.trade_units,
       bo.asset_id,
       bi.investor_id AS buyer_id,
       bi.investor_name AS buyer_name,
       si.investor_id AS seller_id,
       si.investor_name AS seller_name
FROM TRADE t
JOIN TRADE_ORDER bo ON t.buy_order_id = bo.order_id
JOIN TRADE_ORDER so ON t.sell_order_id = so.order_id
JOIN INVESTOR bi ON bo.investor_id = bi.investor_id
JOIN INVESTOR si ON so.investor_id = si.investor_id;

--Q17 investor's name, id and email along with the current wallet balance they have
SELECT investor_id, investor_name, email, wallet_balance
FROM INVESTOR
ORDER BY wallet_balance DESC;

--Q18 Show all wallet transactions with investor name
SELECT wt.transaction_id, i.investor_name, wt.amount, wt.transaction_type, wt.transfer_category, wt.transaction_date
FROM WALLET_TRANSACTION wt
JOIN INVESTOR i ON i.investor_id = wt.investor_id
ORDER BY wt.transaction_date DESC;

--Q19 Total amount transacted per investor grouped by transaction type
SELECT i.investor_name, wt.transaction_type, SUM(wt.amount) AS total_amount
FROM WALLET_TRANSACTION wt
JOIN INVESTOR i ON i.investor_id = wt.investor_id
GROUP BY i.investor_id, wt.transaction_type;

--Q20 Investors who have never made any wallet transaction
SELECT i.investor_id, i.investor_name
FROM INVESTOR i WHERE NOT EXISTS (
    SELECT 1
    FROM WALLET_TRANSACTION wt
    WHERE wt.investor_id = i.investor_id
);

--Q21 Investors whose wallet balance is greater than zero (active wallets)
SELECT investor_id, investor_name, wallet_balance
FROM INVESTOR
WHERE wallet_balance > 0;