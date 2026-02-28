#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="${SCRIPT_DIR}/../../01-infrastructure/00-compose-all"
ENV_FILE="${SCRIPT_DIR}/.env"
DEFAULT_ENV_FILE="${SCRIPT_DIR}/default.env"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_env() {
    if [ ! -f "$ENV_FILE" ]; then
        log_warn ".env file not found: $ENV_FILE"
        if [ -f "$DEFAULT_ENV_FILE" ]; then
            log_info "Copying default.env to .env..."
            cp "$DEFAULT_ENV_FILE" "$ENV_FILE"
            log_success ".env file created from default.env"
        else
            log_error "default.env not found. Please create it first."
            return 1
        fi
    fi
    return 0
}

do_up() {
    log_info "Starting development environment..."
    
    if ! check_env; then
        exit 1
    fi
    
    cd "$COMPOSE_DIR"
    docker-compose -f docker-compose.dev.yml --env-file "$ENV_FILE" up -d
    
    log_info "Checking service status..."
    docker-compose -f docker-compose.dev.yml --env-file "$ENV_FILE" ps
    
    log_success "Development environment started!"
    log_info "MySQL: localhost:3306"
    log_info "Redis: localhost:6379"
}

do_down() {
    log_info "Stopping development environment..."
    
    if ! check_env; then
        exit 1
    fi
    
    cd "$COMPOSE_DIR"
    docker-compose -f docker-compose.dev.yml --env-file "$ENV_FILE" down
    
    log_success "Development environment stopped!"
}

do_ps() {
    if ! check_env; then
        exit 1
    fi
    
    cd "$COMPOSE_DIR"
    docker-compose -f docker-compose.dev.yml --env-file "$ENV_FILE" ps
}

do_logs() {
    local SERVICE="${1:-}"
    
    if ! check_env; then
        exit 1
    fi
    
    cd "$COMPOSE_DIR"
    
    if [ -z "$SERVICE" ]; then
        docker-compose -f docker-compose.dev.yml --env-file "$ENV_FILE" logs -f
    else
        docker-compose -f docker-compose.dev.yml --env-file "$ENV_FILE" logs -f "$SERVICE"
    fi
}

show_help() {
    echo "Usage: $0 [action] [service]"
    echo ""
    echo "Actions:"
    echo "  up              Start dev services [default]"
    echo "  down            Stop dev services"
    echo "  ps              Show status"
    echo "  logs [service]  Show logs"
    echo ""
    echo "Examples:"
    echo "  $0              # Start services"
    echo "  $0 down         # Stop services"
    echo "  $0 logs mysql   # Show MySQL logs"
}

ACTION="${1:-up}"

case $ACTION in
    -h|--help)
        show_help
        exit 0
        ;;
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
        do_logs "$2"
        ;;
    *)
        log_error "Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac
