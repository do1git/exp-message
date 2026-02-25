-- Add role column to users table (시스템 레벨 역할)
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE INDEX idx_users_role ON users (role);
