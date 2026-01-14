#!/bin/bash
# =============================================================================
# Docker Image Build and Push Script
# Reads values.yaml and builds/pushes each image to its registry.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALUES_FILE="${SCRIPT_DIR}/values.yaml"
BACKEND_DIR="${SCRIPT_DIR}/../../02-backend"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Log functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Extract value from values.yaml (without yq, using grep/awk)
get_yaml_value() {
    local key_path="$1"
    local file="$2"
    
    # Simple YAML parsing (supports nested keys)
    # e.g., "app-monolitic.image.registry" -> image.registry value in app-monolitic section
    
    local section=$(echo "$key_path" | cut -d'.' -f1)
    local subkeys=$(echo "$key_path" | cut -d'.' -f2-)
    
    # Find section and extract value
    awk -v section="$section" -v subkeys="$subkeys" '
    BEGIN { in_section = 0; indent = 0; found = 0 }
    
    # Detect section start
    $0 ~ "^"section":" { in_section = 1; next }
    
    # Exit when another top-level section starts
    in_section && /^[a-zA-Z]/ && !/^[[:space:]]/ { in_section = 0 }
    
    in_section {
        # Parse subkeys
        split(subkeys, keys, ".")
        key1 = keys[1]
        key2 = keys[2]
        
        # Find image: section
        if (key1 == "image" && $0 ~ /^[[:space:]]+image:/) {
            in_image = 1
            next
        }
        
        # Find value in image section
        if (in_image && $0 ~ "^[[:space:]]+"key2":") {
            gsub(/.*:[[:space:]]*/, "")
            gsub(/[[:space:]]*#.*$/, "")  # Remove comments
            gsub(/["\047]/, "")  # Remove quotes
            gsub(/[[:space:]]*$/, "")  # Remove trailing whitespace
            print
            found = 1
            exit
        }
        
        # Exit image section when another starts
        if (in_image && /^[[:space:]][[:space:]][a-zA-Z]/ && !/^[[:space:]][[:space:]][[:space:]]/) {
            in_image = 0
        }
    }
    ' "$file"
}

# Build and push function (for background execution)
build_and_push() {
    local name="$1"
    local registry="$2"
    local repository="$3"
    local tag="$4"
    local source_dir="$5"
    local log_file="$6"
    
    {
        if [ -z "$registry" ] || [ -z "$repository" ]; then
            echo -e "${YELLOW}[WARN]${NC} [$name] Registry or repository not configured. Skipping."
            exit 0
        fi
        
        if [ ! -d "$source_dir" ]; then
            echo -e "${YELLOW}[WARN]${NC} [$name] Source directory does not exist: $source_dir. Skipping."
            exit 0
        fi
        
        local full_image="${registry}/${repository}:${tag}"
        
        echo -e "${BLUE}[INFO]${NC} [$name] Starting build: $full_image"
        echo -e "${BLUE}[INFO]${NC} [$name] Source directory: $source_dir"
        
        # Build
        if ! docker build -t "${repository}:${tag}" "$source_dir" 2>&1; then
            echo -e "${RED}[ERROR]${NC} [$name] Build failed!"
            exit 1
        fi
        
        # Tag
        docker tag "${repository}:${tag}" "$full_image"
        
        # Push
        echo -e "${BLUE}[INFO]${NC} [$name] Pushing image: $full_image"
        if ! docker push "$full_image" 2>&1; then
            echo -e "${RED}[ERROR]${NC} [$name] Push failed!"
            exit 1
        fi
        
        echo -e "${GREEN}[SUCCESS]${NC} [$name] Completed: $full_image"
    } > "$log_file" 2>&1
}

# Main execution
main() {
    log_info "=========================================="
    log_info "Docker Build and Push Script (Parallel)"
    log_info "=========================================="
    
    # Check values.yaml exists
    if [ ! -f "$VALUES_FILE" ]; then
        log_error "values.yaml not found: $VALUES_FILE"
        log_info "Copy values.yaml.example to values.yaml first."
        exit 1
    fi
    
    log_info "Config file: $VALUES_FILE"
    echo ""
    
    # Extract image settings
    APP_REGISTRY=$(get_yaml_value "app-monolitic.image.registry" "$VALUES_FILE")
    APP_REPOSITORY=$(get_yaml_value "app-monolitic.image.repository" "$VALUES_FILE")
    APP_TAG=$(get_yaml_value "app-monolitic.image.tag" "$VALUES_FILE")
    APP_TAG="${APP_TAG:-latest}"
    
    MIG_REGISTRY=$(get_yaml_value "batch-db-migration.image.registry" "$VALUES_FILE")
    MIG_REPOSITORY=$(get_yaml_value "batch-db-migration.image.repository" "$VALUES_FILE")
    MIG_TAG=$(get_yaml_value "batch-db-migration.image.tag" "$VALUES_FILE")
    MIG_TAG="${MIG_TAG:-latest}"
    
    # Display parsed image settings
    log_info "Image configurations:"
    log_info "  [app]       ${APP_REGISTRY}/${APP_REPOSITORY}:${APP_TAG}"
    log_info "  [migration] ${MIG_REGISTRY}/${MIG_REPOSITORY}:${MIG_TAG}"
    echo ""
    
    # Create log files in .log directory
    LOG_DIR="${SCRIPT_DIR}/.log"
    mkdir -p "$LOG_DIR"
    APP_LOG="${LOG_DIR}/docker-build-n-push-app.log"
    MIG_LOG="${LOG_DIR}/docker-build-n-push-migration.log"
    > "$APP_LOG"  # Clear/create file
    > "$MIG_LOG"  # Clear/create file
    
    log_info "Starting parallel builds..."
    echo ""
    
    # Show log file paths for real-time monitoring
    log_info "Log files (use 'tail -f <path>' to monitor):"
    log_info "  [app]       $APP_LOG"
    log_info "  [migration] $MIG_LOG"
    echo ""
    
    # Run parallel builds
    build_and_push "app" "$APP_REGISTRY" "$APP_REPOSITORY" "$APP_TAG" "${BACKEND_DIR}/00-monolitic" "$APP_LOG" &
    APP_PID=$!
    
    build_and_push "migration" "$MIG_REGISTRY" "$MIG_REPOSITORY" "$MIG_TAG" "${BACKEND_DIR}/01-db-migrations" "$MIG_LOG" &
    MIG_PID=$!
    
    log_info "[app] Building... (PID: $APP_PID)"
    log_info "[migration] Building... (PID: $MIG_PID)"
    echo ""
    
    # Monitor progress while waiting
    log_info "Progress (updates every 3 seconds):"
    while kill -0 $APP_PID 2>/dev/null || kill -0 $MIG_PID 2>/dev/null; do
        # Get last meaningful line from each log
        APP_STATUS="waiting..."
        MIG_STATUS="waiting..."
        
        if [ -f "$APP_LOG" ] && [ -s "$APP_LOG" ]; then
            APP_LAST=$(tail -1 "$APP_LOG" 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g' | cut -c1-60)
            [ -n "$APP_LAST" ] && APP_STATUS="$APP_LAST"
        fi
        
        if [ -f "$MIG_LOG" ] && [ -s "$MIG_LOG" ]; then
            MIG_LAST=$(tail -1 "$MIG_LOG" 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g' | cut -c1-60)
            [ -n "$MIG_LAST" ] && MIG_STATUS="$MIG_LAST"
        fi
        
        # Check if processes are still running
        APP_RUNNING=$(kill -0 $APP_PID 2>/dev/null && echo "running" || echo "done")
        MIG_RUNNING=$(kill -0 $MIG_PID 2>/dev/null && echo "running" || echo "done")
        
        echo -e "  ${BLUE}[app]${NC} (${APP_RUNNING}) ${APP_STATUS}"
        echo -e "  ${BLUE}[mig]${NC} (${MIG_RUNNING}) ${MIG_STATUS}"
        echo ""
        
        sleep 3
    done
    
    # Wait for all processes and get exit codes
    APP_EXIT=0
    MIG_EXIT=0
    
    wait $APP_PID 2>/dev/null || APP_EXIT=$?
    wait $MIG_PID 2>/dev/null || MIG_EXIT=$?
    
    # Check final result
    if [ $APP_EXIT -ne 0 ] || [ $MIG_EXIT -ne 0 ]; then
        log_error "=========================================="
        log_error "Some builds failed!"
        [ $APP_EXIT -ne 0 ] && log_error "  - app build failed (exit: $APP_EXIT)"
        [ $MIG_EXIT -ne 0 ] && log_error "  - migration build failed (exit: $MIG_EXIT)"
        log_error "=========================================="
        exit 1
    fi
    
    log_success "=========================================="
    log_success "All images built and pushed successfully!"
    log_success "=========================================="
}

# Help
if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help    Show this help message"
    echo ""
    echo "Description:"
    echo "  Reads image settings from values.yaml and builds/pushes"
    echo "  each image to its configured registry."
    echo ""
    echo "Prerequisites:"
    echo "  - Docker must be installed"
    echo "  - values.yaml file must exist"
    echo "  - Registry authentication must be configured"
    exit 0
fi

main "$@"
