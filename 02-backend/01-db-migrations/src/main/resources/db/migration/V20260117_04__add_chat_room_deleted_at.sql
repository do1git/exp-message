ALTER TABLE chat_rooms ADD COLUMN deleted_at DATETIME(6) NULL;
CREATE INDEX idx_chat_rooms_deleted_at ON chat_rooms (deleted_at);
