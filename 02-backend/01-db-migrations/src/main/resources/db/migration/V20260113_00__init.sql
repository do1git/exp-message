-- Initial schema based on JPA Entities from 00-monolitic
-- Generated: 2026-01-13

-- Users table
CREATE TABLE users (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_email ON users (email);

-- Chat rooms table
CREATE TABLE chat_rooms (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_by_user_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_created_by_user_id ON chat_rooms (created_by_user_id);

-- Chat room members table
CREATE TABLE chat_room_members (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    chat_room_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    joined_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_chat_room_id ON chat_room_members (chat_room_id);
CREATE INDEX idx_user_id ON chat_room_members (user_id);
CREATE UNIQUE INDEX idx_chat_room_user ON chat_room_members (chat_room_id, user_id);

-- Messages table
CREATE TABLE messages (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    chat_room_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_messages_chat_room_id ON messages (chat_room_id);
CREATE INDEX idx_chat_room_created_at ON messages (chat_room_id, created_at);
CREATE INDEX idx_messages_user_id ON messages (user_id);
