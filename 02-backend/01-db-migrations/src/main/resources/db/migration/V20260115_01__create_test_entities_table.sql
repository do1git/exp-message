-- Create test_entities table for soft delete testing
-- Generated: 2026-01-15

CREATE TABLE test_entities (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL
);

CREATE INDEX idx_test_name ON test_entities (name);
CREATE INDEX idx_test_deleted_at ON test_entities (deleted_at);
