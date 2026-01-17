ALTER TABLE messages ADD COLUMN deleted_at DATETIME(6) NULL;
CREATE INDEX idx_messages_deleted_at ON messages (deleted_at);
