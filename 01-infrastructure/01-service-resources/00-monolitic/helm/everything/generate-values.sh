#!/bin/bash

# Script to generate Helm values.yaml from .env file
# Usage: ./generate-values.sh [env-file] [output-file]
#   env-file: Path to .env file (default: ../../default.env)
#   output-file: Path to output values.yaml (default: values.yaml)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/../../default.env}"
OUTPUT_FILE="${2:-${SCRIPT_DIR}/values.yaml}"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env file not found at $ENV_FILE"
    exit 1
fi

echo "Reading .env file from: $ENV_FILE"
echo "Generating values.yaml at: $OUTPUT_FILE"

# Read .env file
source "$ENV_FILE"

# Extract image repository and tag
IMAGE_REPO="${MYSQL_IMAGE%%:*}"
IMAGE_TAG="${MYSQL_IMAGE##*:}"
if [ "$IMAGE_REPO" = "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

# Generate values.yaml
cat > "$OUTPUT_FILE" <<EOF
# Global settings applied to all subcharts
global:
  namespace: default
  # Common labels for all resources
  labels: {}
  # Common annotations for all resources
  annotations: {}

# MySQL subchart configuration
mysql:
  enabled: true
  # Override default values from mysql/values.yaml
  image:
    repository: ${IMAGE_REPO}
    tag: "${IMAGE_TAG}"
    pullPolicy: IfNotPresent
  
  mysql:
    rootPassword: ${MYSQL_ROOT_PASSWORD}
    database: ${MYSQL_DATABASE}
    user: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
    port: ${MYSQL_PORT}
  
  binlog:
    enabled: true
    format: ${MYSQL_BINLOG_FORMAT}
    expireLogsDays: ${MYSQL_BINLOG_EXPIRE_DAYS}
    maxBinlogSize: ${MYSQL_BINLOG_MAX_SIZE}
  
  performance:
    innodbBufferPoolSize: ${MYSQL_INNODB_BUFFER_POOL_SIZE}
    innodbLogFileSize: ${MYSQL_INNODB_LOG_FILE_SIZE}
  
  resources:
    requests:
      memory: "256Mi"
      cpu: "250m"
    limits:
      memory: "512Mi"
      cpu: "500m"
  
  persistence:
    enabled: true
    storageClass: ""
    accessMode: ReadWriteOnce
    size: 10Gi
    # HostPath for local development (optional)
    # Uncomment and set path to mount host directory
    # hostPath: "/data/mysql"
  
  service:
    type: ClusterIP
    port: ${MYSQL_PORT}
  
  healthCheck:
    liveness:
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3
    readiness:
      initialDelaySeconds: 10
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 3
  
  replicas: 1
  namespace: default

# Redis subchart configuration (to be added)
# redis:
#   enabled: false
#   # Redis configuration here

# Application subchart configuration (to be added)
# application:
#   enabled: false
#   # Application configuration here
EOF

echo "values.yaml generated successfully!"

