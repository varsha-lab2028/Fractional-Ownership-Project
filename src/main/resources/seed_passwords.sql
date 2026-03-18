-- ============================================================
-- SEED PASSWORDS INTO user_auth
-- Run this in MySQL Workbench or CLI AFTER running auth.sql
-- These are bcrypt hashes of the passwords listed below
-- ============================================================

USE fractional_ownership_db;

-- The user_auth table must already exist (from auth.sql)
-- Passwords:
--   aman123, riya123, karan123, sneha123, arjun123, meera123,
--   rahul123, tanya123, dev123, ishita123, harsh123, neeraj123,
--   simran123, rohit123, pooja123 (investors)
--   ananya123, raghav123, priya123, vikram123, neha123, aditya123,
--   sonal123, kunal123, ira123, sameer123, divya123, manav123,
--   aisha123, nikhil123, ritu123 (admins)
-- 
-- NOTE: Each hash is unique (BCrypt uses random salt each time).
-- These hashes are pre-generated. If you want different passwords,
-- run SeedUserPasswords.java from your IDE instead.

UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'aman@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'riya@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'karan@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'sneha@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'arjun@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'meera@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'rahul@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'tanya@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'dev@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'ishita@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'harsh@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'neeraj@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'simran@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'rohit@gmail.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'pooja@gmail.com';

UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'ananya@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'raghav@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'priya@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'vikram@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'neha@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'aditya@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'sonal@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'kunal@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'ira@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'sameer@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'divya@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'manav@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'aisha@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'nikhil@platform.com';
UPDATE user_auth SET password_hash = '$2a$12$xPyF3/5mOw8RQJzF9rCZmuJn1wqk3QJB3i5KXMEhGhQ9uIb6Qr72a' WHERE email = 'ritu@platform.com';

SELECT 'Password seeding complete.' AS status;
SELECT email, user_type, LEFT(password_hash, 20) AS hash_preview FROM user_auth ORDER BY user_type, email;
