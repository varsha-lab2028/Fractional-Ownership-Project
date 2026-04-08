-- ============================================================
-- SUPABASE / POSTGRES VERSION OF auth.sql
-- ============================================================
-- Run this AFTER supabase_schema_and_data.sql
-- ============================================================

-- Drop and recreate user_auth
DROP TABLE IF EXISTS user_auth CASCADE;

CREATE TABLE user_auth (
    auth_id       SERIAL PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL DEFAULT 'UNSET',
    user_type     VARCHAR(10) NOT NULL CHECK (user_type IN ('ADMIN', 'INVESTOR')),
    linked_id     INT NOT NULL,
    status        VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISABLED')),
    last_login    TIMESTAMP NULL,
    password_created TIMESTAMP NOT NULL DEFAULT NOW(),
    password_updated TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Insert INVESTOR rows (password will be set by SeedUserPasswords.java)
INSERT INTO user_auth (email, password_hash, user_type, linked_id, status)
SELECT email, 'UNSET', 'INVESTOR', investor_id, 'ACTIVE'
FROM investor;

-- Insert ADMIN rows
INSERT INTO user_auth (email, password_hash, user_type, linked_id, status)
SELECT email, 'UNSET', 'ADMIN', admin_id, 'ACTIVE'
FROM admin;

-- Verify
SELECT COUNT(*) AS total_rows FROM user_auth;
SELECT user_type, COUNT(*) AS count FROM user_auth GROUP BY user_type;
