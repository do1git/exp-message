-- Channel conversations table (채널별 상담 세션, ChatRoom id와 1:1)
CREATE TABLE channel_conversations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL
);

CREATE INDEX idx_channel_id ON channel_conversations (channel_id);
CREATE INDEX idx_customer_id ON channel_conversations (customer_id);
CREATE INDEX idx_channel_conversations_deleted_at ON channel_conversations (deleted_at);
