#!/bin/bash
# =============================================================================
# Docker Compose Deployment Script
# Manages docker-compose services for local integrated environment
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="${SCRIPT_DIR}/../../01-infrastructure/00-compose-all"
ENV_FILE="${SCRIPT_DIR}/.env"

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

# Check .env file
check_env() {
    if [ ! -f "$ENV_FILE" ]; then
        log_warn ".env file not found: $ENV_FILE"
        log_info "Copy default.env from 01-infrastructure/00-compose-all to .env first."
        log_info "Example: cp ${COMPOSE_DIR}/default.env ${ENV_FILE}"
        return 1
    fi
    return 0
}

# Up (start services)
do_up() {
    log_info "=========================================="
    log_info "Docker Compose Up Started"
    log_info "=========================================="
    
    if ! check_env; then
        exit 1
    fi
    
    log_info "Compose directory: $COMPOSE_DIR"
    log_info "Environment file: $ENV_FILE"
    
    cd "$COMPOSE_DIR"
    docker-compose --env-file "$ENV_FILE" up -d
    
    log_info "Checking service status..."
    docker-compose --env-file "$ENV_FILE" ps
    
    log_success "=========================================="
    log_success "Docker Compose Up Completed!"
    log_success "=========================================="
}

# Down (stop services)
do_down() {
    log_info "=========================================="
    log_info "Docker Compose Down Started"
    log_info "=========================================="
    
    if ! check_env; then
        exit 1
    fi
    
    log_info "Compose directory: $COMPOSE_DIR"
    log_info "Environment file: $ENV_FILE"
    
    cd "$COMPOSE_DIR"
    docker-compose --env-file "$ENV_FILE" down
    
    log_success "=========================================="
    log_success "Docker Compose Down Completed!"
    log_success "=========================================="
}

# Status
do_ps() {
    if ! check_env; then
        exit 1
    fi
    
    cd "$COMPOSE_DIR"
    docker-compose --env-file "$ENV_FILE" ps
}

# Logs
do_logs() {
    local SERVICE="${1:-}"
    
    if ! check_env; then
        exit 1
    fi
    
    cd "$COMPOSE_DIR"
    
    if [ -z "$SERVICE" ]; then
        log_info "Showing logs for all services (Press Ctrl+C to exit)"
        docker-compose --env-file "$ENV_FILE" logs -f
    else
        log_info "Showing logs for service: $SERVICE (Press Ctrl+C to exit)"
        docker-compose --env-file "$ENV_FILE" logs -f "$SERVICE"
    fi
}

# Restart
do_restart() {
    local SERVICE="${1:-}"
    
    log_info "=========================================="
    log_info "Docker Compose Restart"
    log_info "=========================================="
    
    if ! check_env; then
        exit 1
    fi
    
    cd "$COMPOSE_DIR"
    
    if [ -z "$SERVICE" ]; then
        log_info "Restarting all services..."
        docker-compose --env-file "$ENV_FILE" restart
    else
        log_info "Restarting service: $SERVICE"
        docker-compose --env-file "$ENV_FILE" restart "$SERVICE"
    fi
    
    log_success "Restart completed!"
}

# Help
show_help() {
    echo "Usage: $0 [action] [service]"
    echo ""
    echo "Options:"
    echo "  -h, --help       Show this help message"
    echo ""
    echo "Actions:"
    echo "  up              Start all services [default]"
    echo "  down            Stop all services"
    echo "  ps              Show service status"
    echo "  logs [service]  Show logs (all services or specific service)"
    echo "  restart [service]  Restart services (all or specific)"
    echo ""
    echo "Examples:"
    echo "  $0              # Start all services (default)"
    echo "  $0 up           # Start all services"
    echo "  $0 down         # Stop all services"
    echo "  $0 ps           # Show status"
    echo "  $0 logs         # Show all logs"
    echo "  $0 logs mysql   # Show mysql logs"
    echo "  $0 restart      # Restart all services"
    echo "  $0 restart nginx # Restart nginx service"
}

# Main
ACTION=""
SERVICE=""

# Parse arguments
if [[ $# -gt 0 ]]; then
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            ACTION="$1"
            SERVICE="$2"
            ;;
    esac
fi

if [ -z "$ACTION" ]; then
    log_info "No action specified. Using default 'up'."
    ACTION="up"
fi

case $ACTION in
    up)
        do_up
        ;;
    down)
        do_down
        ;;
    ps)
        do_ps
        ;;
    logs)
        do_logs "$SERVICE"
        ;;
    restart)
        do_restart "$SERVICE"
        ;;
    *)
        log_error "Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac
