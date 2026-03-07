USE fractional_ownership_db;

DROP TABLE IF EXISTS `user_auth`;
CREATE TABLE user_auth (
    auth_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    user_type ENUM('ADMIN', 'INVESTOR') NOT NULL,
    linked_id INT NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    last_login DATETIME NULL,
    password_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    password_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO user_auth (email, password_hash, user_type, linked_id, status)
SELECT email, password_hash, 'ADMIN', admin_id, 'ACTIVE'
FROM admin;

INSERT INTO user_auth (email, password_hash, user_type, linked_id, status)
SELECT email, password_hash, 'INVESTOR', investor_id, 'ACTIVE'
FROM investor;

---verifying the data
SELECT COUNT(*) FROM user_auth;
SELECT user_type, COUNT(*) FROM user_auth GROUP BY user_type;
SELECT * FROM user_auth;