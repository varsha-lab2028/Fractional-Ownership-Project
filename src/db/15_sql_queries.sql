USE fractional_ownership_db; 

--typical 'SELECT' queries for the tables 
SELECT * FROM 'ADMIN'; 
SELECT * FROM 'ASSET'; 
SELECT * FROM 'INVESTOR';
SELECT * FROM 'IPO'; 
SELECT * FROM 'OWNERSHIP';
SELECT * FROM 'TRADE_ORDER'; 
SELECT * FROM 'TRADE';
SELECT * FROM 'OWNERSHIP_HISTORY'; 
SELECT * FROM 'VALUATION'; 

--15 SQL queries 
--Q1 
SELECT asset_id, name, category, storage_location, verification_status FROM ASSET WHERE verification_status = 'Verified';

--Q2 
SELECT a.asset_id, a.name AS asset_name, a.verification_status, ad.admin_id, ad.name AS admin_name FROM ASSET a LEFT JOIN ADMIN ad ON ad.admin_id = a.verified_by;

--Q3
SELECT i.ipo_id, a.name AS asset_name, i.total_units, i.price_per_unit FROM IPO i JOIN ASSET a ON a.asset_id = i.asset_id;

--Q4
SELECT * FROM IPO WHERE CURDATE() BETWEEN ipo_start_date AND ipo_end_date; 

--Q5
SELECT o.investor_id, inv.name, a.name AS asset_name, o.units_held FROM OWNERSHIP o JOIN INVESTOR inv ON inv.investor_id = o.investor_id JOIN ASSET a ON a.asset_id = o.asset_id;

--Q6
SELECT asset_id, SUM(units_held) AS total_units FROM OWNERSHIP GROUP BY asset_id;

--Q7
SELECT investor_id, SUM(units_held) AS total_units FROM OWNERSHIP GROUP BY investor_id ORDER BY total_units DESC;

--Q8
SELECT a.asset_id, a.name, v.valuation_amount FROM ASSET a LEFT JOIN VALUATION v ON a.asset_id = v.asset_id;

--Q9 
SELECT asset_id FROM ASSET a WHERE NOT EXISTS (SELECT 1 FROM TRADE_ORDER o WHERE o.asset_id = a.asset_id);

--Q10
SELECT investor_id FROM INVESTOR i WHERE EXISTS (SELECT 1 FROM TRADE_ORDER o WHERE o.investor_id = i.investor_id);

--Q11
SELECT investor_id FROM OWNERSHIP UNION SELECT investor_id FROM TRADE_ORDER;

--Q12
SELECT i.asset_id, i.total_units, SUM(o.units_held) AS total_held FROM IPO i LEFT JOIN OWNERSHIP o ON o.asset_id = i.asset_id GROUP BY i.asset_id, i.total_units HAVING SUM(o.units_held) > i.total_units;

--Q13
SELECT investor_id FROM OWNERSHIP WHERE investor_id NOT IN (SELECT investor_id FROM TRADE_ORDER);

--Q14 Show latest valuation of each asset
SELECT v.asset_id, a.name AS asset_name, v.valuation_amount, v.valuation_date FROM VALUATION v JOIN ASSET a ON v.asset_id = a.asset_id
WHERE (v.asset_id, v.valuation_date) IN (SELECT asset_id, MAX(valuation_date) FROM VALUATION GROUP BY asset_id);

--Q15 Show matched trades with buyer and seller investors
SELECT t.trade_id, t.trade_date, t.trade_price, t.trade_units, b.asset_id FROM TRADE t JOIN TRADE_ORDER b ON t.buy_order_id = b.order_id;
