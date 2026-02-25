-- Channels table (서비스 단위, 위젯 연동 대상)
CREATE TABLE channels (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_channels_api_key UNIQUE (api_key)
);

CREATE INDEX idx_channels_deleted_at ON channels (deleted_at);
