-- Channel operators table (채널별 상담원)
CREATE TABLE channel_operators (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_channel_operators_channel_user UNIQUE (channel_id, user_id)
);

CREATE INDEX idx_channel_id ON channel_operators (channel_id);
CREATE INDEX idx_channel_operators_user_id ON channel_operators (user_id);
CREATE INDEX idx_channel_operators_deleted_at ON channel_operators (deleted_at);
