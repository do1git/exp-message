#!/bin/bash

# Script to generate docker-compose.yml from .env file
# Usage: ./generate-docker-compose.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/../.env"
OUTPUT_FILE="${SCRIPT_DIR}/docker-compose.yml"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env file not found at $ENV_FILE"
    exit 1
fi

echo "Reading .env file from: $ENV_FILE"
echo "Generating docker-compose.yml at: $OUTPUT_FILE"

# Create required directories
MYSQL_DIR="${SCRIPT_DIR}/mysql"
DATA_DIR="${MYSQL_DIR}/data"
CONF_D_DIR="${MYSQL_DIR}/conf.d"
INIT_DIR="${MYSQL_DIR}/init"

mkdir -p "$DATA_DIR"
mkdir -p "$CONF_D_DIR"
mkdir -p "$INIT_DIR"

echo "Created directories: $DATA_DIR, $CONF_D_DIR, $INIT_DIR"

# Create default my.cnf file if it doesn't exist
MY_CNF_FILE="${CONF_D_DIR}/my.cnf"
if [ ! -f "$MY_CNF_FILE" ]; then
    cat > "$MY_CNF_FILE" <<'CNFEOF'
[mysqld]
# Binlog configuration
log-bin=mysql-bin
binlog_format=ROW
expire_logs_days=7
max_binlog_size=100M
binlog_cache_size=1M
max_binlog_cache_size=2M

# Server ID (required for replication)
server-id=1

# Performance settings
innodb_buffer_pool_size=256M
innodb_log_file_size=64M

# Character set configuration
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

[mysql]
default-character-set=utf8mb4

[client]
default-character-set=utf8mb4
CNFEOF
    echo "Created default my.cnf file: $MY_CNF_FILE"
fi

# Create .gitkeep file for init directory
GITKEEP_FILE="${INIT_DIR}/.gitkeep"
if [ ! -f "$GITKEEP_FILE" ]; then
    touch "$GITKEEP_FILE"
    echo "Created .gitkeep file: $GITKEEP_FILE"
fi

# Read .env file
source "$ENV_FILE"

# Generate docker-compose.yml
cat > "$OUTPUT_FILE" <<EOF
services:
  mysql:
    image: \${MYSQL_IMAGE:-${MYSQL_IMAGE}}
    container_name: \${MYSQL_CONTAINER_NAME:-${MYSQL_CONTAINER_NAME}}
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: \${MYSQL_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD}}
      MYSQL_DATABASE: \${MYSQL_DATABASE:-${MYSQL_DATABASE}}
      MYSQL_USER: \${MYSQL_USER:-${MYSQL_USER}}
      MYSQL_PASSWORD: \${MYSQL_PASSWORD:-${MYSQL_PASSWORD}}
    ports:
      - "\${MYSQL_HOST_PORT:-${MYSQL_HOST_PORT}}:\${MYSQL_PORT:-${MYSQL_PORT}}"
    volumes:
      - ./mysql/data:/var/lib/mysql
      - ./mysql/conf.d:/etc/mysql/conf.d
      - ./mysql/init:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p\${MYSQL_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD}}"]
      interval: \${MYSQL_HEALTHCHECK_INTERVAL:-${MYSQL_HEALTHCHECK_INTERVAL}}
      timeout: \${MYSQL_HEALTHCHECK_TIMEOUT:-${MYSQL_HEALTHCHECK_TIMEOUT}}
      retries: \${MYSQL_HEALTHCHECK_RETRIES:-${MYSQL_HEALTHCHECK_RETRIES}}

EOF

echo "docker-compose.yml generated successfully!"
