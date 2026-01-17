ALTER TABLE chat_room_members ADD COLUMN deleted_at DATETIME(6) NULL;
CREATE INDEX idx_chat_room_members_deleted_at ON chat_room_members (deleted_at);
