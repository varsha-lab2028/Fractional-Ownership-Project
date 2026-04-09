-- ================================================================
-- task6_conflicts.sql
-- Task 6.8 — Concurrency conflict demonstration
-- Database: Supabase / PostgreSQL
--
-- HOW TO RUN:
--   Open two browser tabs on the Supabase SQL Editor.
--   Tab 1 = Session A,  Tab 2 = Session B.
--   Follow the step-by-step instructions in each scenario.
-- ================================================================


-- ================================================================
-- SCENARIO 1: Lost Update on Ownership
-- ================================================================
-- WITHOUT locking, two sessions both read units_held = 10,
-- both subtract 5, and both commit — leaving 5 instead of 0.
-- One session's update is silently overwritten (lost update).
-- ================================================================

-- ── SETUP (run once in either tab) ───────────────────────────────
-- Give investor 5 exactly 10 units of asset 6 for this demo
UPDATE ownership SET units_held = 10 WHERE investor_id = 5 AND asset_id = 6;

-- Verify before state (should show 10)
SELECT investor_id, asset_id, units_held AS "BEFORE (should be 10)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;


-- ════════════════════════════════════════════════════════════════
-- WITHOUT FIX — run these blocks to observe the anomaly
-- ════════════════════════════════════════════════════════════════

-- ── SESSION A — Tab 1 ────────────────────────────────────────────
-- Step 1: Start transaction and read units (sees 10)
BEGIN;
SELECT units_held AS "Session A reads (expects 10)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;
-- ⏸ PAUSE HERE. Do NOT run the next line yet. Switch to Tab 2.

-- Step 3: (after Session B commits) — Session A writes stale value
UPDATE ownership SET units_held = units_held - 5
WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;
-- Session A commits 10-5 = 5, silently overwriting Session B's committed 5.


-- ── SESSION B — Tab 2 ────────────────────────────────────────────
-- Step 2: While Session A is paused, run Session B completely
BEGIN;
SELECT units_held AS "Session B reads (also sees 10 — stale snapshot)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;
UPDATE ownership SET units_held = units_held - 5
WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;
-- Session B commits 10-5 = 5. Now switch back to Tab 1 and run Step 3.


-- ── AFTER STATE — anomaly ────────────────────────────────────────
-- Run in either tab after both sessions commit
SELECT units_held AS "AFTER without fix (WRONG: shows 5, should be 0)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;

-- ── EXPLANATION ──────────────────────────────────────────────────
-- Both sessions read units_held = 10 before either committed.
-- Session B committed 10-5 = 5 first.
-- Session A then overwrote that with its own stale 10-5 = 5.
-- Session B's deduction was silently lost.
-- Investor 5 should hold 0 units, but holds 5 — a "lost update".


-- ════════════════════════════════════════════════════════════════
-- WITH FIX — SELECT ... FOR UPDATE prevents the anomaly
-- ════════════════════════════════════════════════════════════════

-- Reset state for the fix demo
UPDATE ownership SET units_held = 10 WHERE investor_id = 5 AND asset_id = 6;
SELECT units_held AS "RESET (should be 10)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;


-- ── SESSION A — Tab 1 (with FOR UPDATE) ──────────────────────────
BEGIN;
SELECT units_held FROM ownership
WHERE  investor_id = 5 AND asset_id = 6
FOR UPDATE;
-- FOR UPDATE acquires an exclusive row lock.
-- Session B is now BLOCKED when it tries its own FOR UPDATE.
-- ⏸ PAUSE HERE. Switch to Tab 2 and run Session B.

UPDATE ownership SET units_held = units_held - 5
WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;
-- Lock released. Session B unblocks and reads 5 (not 10).


-- ── SESSION B — Tab 2 (with FOR UPDATE) ──────────────────────────
BEGIN;
SELECT units_held FROM ownership
WHERE  investor_id = 5 AND asset_id = 6
FOR UPDATE;
-- ← BLOCKS here until Session A commits.
-- After Session A commits: Session B unblocks, reads units_held = 5.
UPDATE ownership SET units_held = units_held - 5
WHERE  investor_id = 5 AND asset_id = 6;
COMMIT;


-- ── AFTER STATE — correct result ─────────────────────────────────
SELECT units_held AS "AFTER with fix (CORRECT: should be 0)"
FROM   ownership WHERE investor_id = 5 AND asset_id = 6;

-- ── WHY LOCKING IS NEEDED ────────────────────────────────────────
-- Without FOR UPDATE, Postgres's default READ COMMITTED isolation
-- lets each session read the last committed value — which is the
-- same row before either session has committed its deduction.
-- FOR UPDATE serialises access: the second session is forced to
-- wait until the first commits, then reads the already-updated value.
-- This prevents the lost update entirely.


-- ================================================================
-- SCENARIO 2: Same Sell Order Executed Twice
-- ================================================================
-- WITHOUT locking, two sessions both see a SELL order as OPEN,
-- both execute a trade against it, and both commit — producing
-- two trade rows for the same order, debiting the seller twice.
-- ================================================================

-- ── SETUP (run once in either tab) ───────────────────────────────
-- Mark sell order #13 as OPEN for this demo
UPDATE trade_order SET status = 'OPEN' WHERE order_id = 13;
DELETE FROM trade WHERE sell_order_id = 13;   -- remove any leftover trades

-- Verify before state
SELECT order_id, status AS "BEFORE (should be OPEN)"
FROM   trade_order WHERE order_id = 13;


-- ════════════════════════════════════════════════════════════════
-- WITHOUT FIX — run to observe the anomaly
-- ════════════════════════════════════════════════════════════════

-- ── SESSION A — Tab 1 ────────────────────────────────────────────
BEGIN;
SELECT status AS "Session A reads (should be OPEN)"
FROM   trade_order WHERE order_id = 13;
-- Session A sees OPEN — proceeds to execute trade
INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
VALUES (
    (SELECT COALESCE(MAX(trade_id), 0) + 1 FROM trade),
    500.00, 5, CURRENT_DATE, 3, 13
);
-- ⏸ PAUSE. Do NOT commit yet. Switch to Tab 2 and run Session B fully.
COMMIT;


-- ── SESSION B — Tab 2 ────────────────────────────────────────────
BEGIN;
SELECT status AS "Session B reads (also sees OPEN — Session A hasn't committed)"
FROM   trade_order WHERE order_id = 13;
-- Session B also sees OPEN — inserts another trade for the SAME order
INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
VALUES (
    (SELECT COALESCE(MAX(trade_id), 0) + 1 FROM trade),
    500.00, 5, CURRENT_DATE, 3, 13
);
COMMIT;
-- Now switch back to Tab 1 and COMMIT Session A.


-- ── AFTER STATE — anomaly ────────────────────────────────────────
SELECT COUNT(*) AS "Trade rows for order 13 (WRONG: should be 1, shows 2)"
FROM   trade WHERE sell_order_id = 13;

-- ── EXPLANATION ──────────────────────────────────────────────────
-- Both sessions read the order status as OPEN before either committed.
-- Both inserted a trade row against the same sell order.
-- The seller's ownership was debited twice — units_held is now negative.
-- The order was "matched" twice but only has one real counterpart.


-- ════════════════════════════════════════════════════════════════
-- WITH FIX — FOR UPDATE + status re-check prevents double execution
-- ════════════════════════════════════════════════════════════════

-- Reset state
UPDATE trade_order SET status = 'OPEN' WHERE order_id = 13;
DELETE FROM trade WHERE sell_order_id = 13;
SELECT order_id, status AS "RESET (should be OPEN)"
FROM   trade_order WHERE order_id = 13;


-- ── SESSION A — Tab 1 (with FOR UPDATE) ──────────────────────────
BEGIN;
SELECT status FROM trade_order WHERE order_id = 13 FOR UPDATE;
-- FOR UPDATE locks the order row exclusively.
-- Session B is now BLOCKED at its FOR UPDATE until Session A commits.
INSERT INTO trade (trade_id, trade_price, trade_units, trade_date, buy_order_id, sell_order_id)
VALUES (
    (SELECT COALESCE(MAX(trade_id), 0) + 1 FROM trade),
    500.00, 5, CURRENT_DATE, 3, 13
);
UPDATE trade_order SET status = 'MATCHED' WHERE order_id = 13;
COMMIT;
-- Lock released. Session B unblocks and reads status = MATCHED.


-- ── SESSION B — Tab 2 (with FOR UPDATE + status check) ───────────
BEGIN;
SELECT status FROM trade_order WHERE order_id = 13 FOR UPDATE;
-- ← BLOCKS here until Session A commits.
-- After Session A commits: unblocks, reads status = MATCHED.
-- Status check — if not OPEN, abort cleanly:
DO $$
DECLARE v_status VARCHAR(20);
BEGIN
    SELECT status INTO v_status FROM trade_order WHERE order_id = 13;
    IF v_status <> 'OPEN' THEN
        RAISE EXCEPTION
            'Order #13 is no longer OPEN (status: %). Trade aborted — no duplicate.', v_status;
    END IF;
END;
$$;
ROLLBACK;
-- Session B rolls back cleanly. No duplicate trade is inserted.


-- ── AFTER STATE — correct result ─────────────────────────────────
SELECT COUNT(*) AS "Trade rows for order 13 (CORRECT: should be 1)"
FROM   trade WHERE sell_order_id = 13;

SELECT order_id, status AS "Order status (CORRECT: should be MATCHED)"
FROM   trade_order WHERE order_id = 13;

-- ── WHY LOCKING IS NEEDED ────────────────────────────────────────
-- Without locking, two sessions both read OPEN before either commits.
-- With FOR UPDATE, the second session is blocked until the first commits.
-- After the first commits and sets status = MATCHED, the second session
-- unblocks, reads MATCHED, and aborts — preventing the duplicate trade.
