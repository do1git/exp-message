-- Test migration: Create test table for Flyway batch testing
-- Generated: 2026-01-14

CREATE TABLE flyway_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

INSERT INTO flyway_test (message) VALUES ('Flyway migration test successful!');
