USE fractional_ownership_db;

-- Drop and recreate user_auth
DROP TABLE IF EXISTS `user_auth`;
CREATE TABLE user_auth (
    auth_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL DEFAULT 'UNSET',
    user_type ENUM('ADMIN', 'INVESTOR') NOT NULL,
    linked_id INT NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    last_login DATETIME NULL,
    password_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    password_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
