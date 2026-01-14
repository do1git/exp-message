#!/bin/bash
# =============================================================================
# Helm Chart Deployment Script
# Supports install, upgrade, uninstall actions.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALUES_FILE="${SCRIPT_DIR}/values.yaml"
KUBECONFIG_FILE="${SCRIPT_DIR}/kubeconfig.yaml"
RELEASE_NAME="message-stack"

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

# Prerequisites check
pre_check() {
    # Check values.yaml exists
    if [ ! -f "$VALUES_FILE" ]; then
        log_error "values.yaml not found: $VALUES_FILE"
        log_info "Copy values.yaml.example to values.yaml first."
        exit 1
    fi
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Config file: $VALUES_FILE"
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Release name: $RELEASE_NAME"
}

# Install (create)
do_install() {
    log_info "=========================================="
    log_info "Helm Install Started"
    log_info "=========================================="
    
    pre_check
    
    log_info "Updating dependencies..."
    helm dependency update --kubeconfig="$KUBECONFIG_FILE" "$SCRIPT_DIR"
    
    log_info "Running Helm Install..."
    helm install "$RELEASE_NAME" "$SCRIPT_DIR" \
        --kubeconfig="$KUBECONFIG_FILE" \
        --values "$VALUES_FILE"
    
    log_info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KUBECONFIG_FILE" \
        -l app.kubernetes.io/instance="$RELEASE_NAME"
    
    log_success "=========================================="
    log_success "Helm Install Completed!"
    log_success "=========================================="
}

# Upgrade (with rollout restart)
do_upgrade() {
    log_info "=========================================="
    log_info "Helm Upgrade Started"
    log_info "=========================================="
    
    pre_check
    
    log_info "Updating dependencies..."
    helm dependency update --kubeconfig="$KUBECONFIG_FILE" "$SCRIPT_DIR"
    
    log_info "Running Helm Upgrade..."
    helm upgrade "$RELEASE_NAME" "$SCRIPT_DIR" \
        --kubeconfig="$KUBECONFIG_FILE" \
        --values "$VALUES_FILE"
    
    log_info "Restarting pod rollout (required for latest tag)..."
    kubectl rollout restart deployment/"${RELEASE_NAME}-app-monolitic" \
        --kubeconfig="$KUBECONFIG_FILE" 2>/dev/null || \
        log_warn "app-monolitic deployment rollout failed (may not exist)"
    
    log_info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KUBECONFIG_FILE" \
        -l app.kubernetes.io/instance="$RELEASE_NAME"
    
    log_success "=========================================="
    log_success "Helm Upgrade Completed!"
    log_success "=========================================="
}

# Logs - App
do_logs_app() {
    log_info "=========================================="
    log_info "App (00-monolitic) Logs"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Press Ctrl+C to exit"
    echo ""
    
    kubectl logs -f -l app.kubernetes.io/name=app-monolitic \
        --kubeconfig="$KUBECONFIG_FILE" \
        --tail=100
}

# Logs - Migration
do_logs_migration() {
    log_info "=========================================="
    log_info "Migration (01-db-migrations) Logs"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Press Ctrl+C to exit"
    echo ""
    
    kubectl logs -f -l app.kubernetes.io/name=batch-db-migration \
        --kubeconfig="$KUBECONFIG_FILE" \
        --tail=100
}

# MySQL Port Forward (expose 3306)
do_mysql_port_forward() {
    local LOCAL_PORT="${1:-13306}"
    
    log_info "=========================================="
    log_info "MySQL Port Forward ($LOCAL_PORT -> 3306)"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Forwarding 0.0.0.0:$LOCAL_PORT (Current PC) -> ${RELEASE_NAME}-mysql:3306"
    
    # Check if mysql command exists
    if command -v mysql &> /dev/null; then
        log_info "MySQL client found. Starting port-forward in background..."
        echo ""
        
        # Start port-forward in background
        kubectl port-forward "svc/${RELEASE_NAME}-mysql" "${LOCAL_PORT}:3306" \
            --address 0.0.0.0 \
            --kubeconfig="$KUBECONFIG_FILE" &
        local PF_PID=$!
        
        # Wait for port to be ready
        sleep 2
        
        log_info "Connecting to MySQL... (type 'exit' to quit)"
        echo ""
        
        # Connect to MySQL
        mysql -h 127.0.0.1 -P "$LOCAL_PORT" -u message_user -pmessage_password message_db
        
        # Cleanup: stop port-forward
        echo ""
        log_info "Stopping port-forward..."
        kill $PF_PID 2>/dev/null
        log_success "Done!"
    else
        log_warn "MySQL client not found. Running port-forward only."
        log_info "Press Ctrl+C to stop port forwarding"
        echo ""
        
        kubectl port-forward "svc/${RELEASE_NAME}-mysql" "${LOCAL_PORT}:3306" \
            --address 0.0.0.0 \
            --kubeconfig="$KUBECONFIG_FILE"
    fi
}

# MySQL Port Forward Only (no shell)
do_mysql_port_forward_only() {
    local LOCAL_PORT="${1:-13306}"
    
    log_info "=========================================="
    log_info "MySQL Port Forward ($LOCAL_PORT -> 3306)"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Forwarding 0.0.0.0:$LOCAL_PORT (Current PC) -> ${RELEASE_NAME}-mysql:3306"
    log_info "Press Ctrl+C to stop port forwarding"
    echo ""
    
    kubectl port-forward "svc/${RELEASE_NAME}-mysql" "${LOCAL_PORT}:3306" \
        --address 0.0.0.0 \
        --kubeconfig="$KUBECONFIG_FILE"
}

# MySQL Port Forward Background
do_mysql_port_forward_background() {
    local LOCAL_PORT="${1:-13306}"
    
    log_info "=========================================="
    log_info "MySQL Port Forward Background ($LOCAL_PORT -> 3306)"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Forwarding 0.0.0.0:$LOCAL_PORT (Current PC) -> ${RELEASE_NAME}-mysql:3306"
    
    # Start port-forward in background
    nohup kubectl port-forward "svc/${RELEASE_NAME}-mysql" "${LOCAL_PORT}:3306" \
        --address 0.0.0.0 \
        --kubeconfig="$KUBECONFIG_FILE" > /dev/null 2>&1 &
    local PF_PID=$!
    
    sleep 1
    
    log_success "Port forward started in background (PID: $PF_PID)"
    log_info "To stop: kill $PF_PID"
    log_info "To find process: ps aux | grep port-forward"
}

# Kubectl with kubeconfig
do_kubectl() {
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    if [ $# -eq 0 ]; then
        log_warn "No kubectl arguments provided."
        log_info "Usage: $0 kubectl [args...]"
        log_info "Example: $0 kubectl get pods"
        return
    fi
    
    log_info "Running: kubectl $* --kubeconfig=..."
    kubectl "$@" --kubeconfig="$KUBECONFIG_FILE"
}

# MySQL Port Forward Background Kill
do_mysql_port_forward_background_kill() {
    log_info "=========================================="
    log_info "MySQL Port Forward Background Kill"
    log_info "=========================================="
    
    # Find kubectl port-forward processes for mysql
    local pids=$(pgrep -f "kubectl port-forward.*mysql" 2>/dev/null)
    
    if [ -z "$pids" ]; then
        log_warn "No MySQL port-forward processes found."
        log_info "Running processes:"
        ps aux | grep -E "port-forward.*mysql" | grep -v grep || echo "  (none)"
        return
    fi
    
    local count=0
    for pid in $pids; do
        log_info "Stopping PID: $pid"
        kill "$pid" 2>/dev/null && ((count++))
    done
    
    log_success "Stopped $count port-forward process(es)."
}

# Uninstall (delete)
do_uninstall() {
    log_info "=========================================="
    log_info "Helm Uninstall Started"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Release name: $RELEASE_NAME"
    
    log_warn "This will delete release '$RELEASE_NAME'. Continue? (y/N)"
    read -r confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log_info "Cancelled."
        exit 0
    fi
    
    log_info "Running Helm Uninstall..."
    helm uninstall "$RELEASE_NAME" --kubeconfig="$KUBECONFIG_FILE"
    
    log_success "=========================================="
    log_success "Helm Uninstall Completed!"
    log_success "=========================================="
}

# Help
show_help() {
    echo "Usage: $0 [action]"
    echo ""
    echo "Options:"
    echo "  -h, --help       Show this help message"
    echo ""
    echo "Actions:"
    echo "  install, c       Install new release"
    echo "  upgrade, u       Upgrade existing release (with pod rollout) [default]"
    echo "  uninstall, d     Delete release"
    echo "  logs-app, la     View App (00-monolitic) logs"
    echo "  logs-migration, lm  View Migration (01-db-migrations) logs"
    echo "  mysql-mono, mm [port]     Connect to MySQL shell (auto port-forward)"
    echo "  mysql-mono-portforward, mmpf [port] Port forward MySQL only (default: 13306)"
    echo "  mysql-mono-portforward-background, mmpfbg [port] Port forward in background"
    echo "  mysql-mono-portforward-background-kill, mmpfbgkill Stop background port forward"
    echo "  kubectl [args...]  Run kubectl with kubeconfig"
    echo ""
    echo "Examples:"
    echo "  $0           # Upgrade (default)"
    echo "  $0 c         # Install"
    echo "  $0 u         # Upgrade"
    echo "  $0 d         # Uninstall"
    echo "  $0 la        # App logs"
    echo "  $0 lm        # Migration logs"
    echo "  $0 mm        # MySQL shell (auto port-forward)"
    echo "  $0 mmpf      # MySQL port forward only (13306)"
    echo "  $0 mmpfbg    # MySQL port forward in background"
    echo "  $0 mmpfbgkill # Stop background port forward"
    echo "  $0 kubectl get pods  # Run kubectl with kubeconfig"
    echo "  $0 kubectl exec -it pod-name -- bash"
}

# Main
ACTION=""
PORT=""

# Parse arguments
if [[ $# -gt 0 ]]; then
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            ACTION="$1"
            PORT="$2"
            ;;
    esac
fi

if [ -z "$ACTION" ]; then
    log_info "No action specified. Using default 'upgrade'."
    ACTION="upgrade"
fi

case $ACTION in
    install|c)
        do_install
        ;;
    upgrade|u)
        do_upgrade
        ;;
    uninstall|d)
        do_uninstall
        ;;
    logs-app|la)
        do_logs_app
        ;;
    logs-migration|lm)
        do_logs_migration
        ;;
    mysql-mono|mm)
        do_mysql_port_forward "$PORT"
        ;;
    mysql-mono-portforward|mmpf)
        do_mysql_port_forward_only "$PORT"
        ;;
    mysql-mono-portforward-background|mmpfbg)
        do_mysql_port_forward_background "$PORT"
        ;;
    mysql-mono-portforward-background-kill|mmpfbgkill)
        do_mysql_port_forward_background_kill
        ;;
    kubectl)
        shift
        do_kubectl "$@"
        exit 0
        ;;
    *)
        log_error "Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac
