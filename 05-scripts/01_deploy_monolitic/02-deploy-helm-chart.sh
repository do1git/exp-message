#!/bin/bash

# Helm chart deployment script for monolitic stack

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --action|-a)
            DEPLOY_ACTION="$2"
            shift 2
            ;;
        --mode|-m)
            DEPLOY_MODE="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -a, --action ACTION          Deployment action: install, upgrade, delete, or install-or-upgrade"
            echo "  -m, --mode MODE              Deployment mode: remote or local"
            echo "  -h, --help                  Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help or -h for usage information"
            exit 1
            ;;
    esac
done

# Load .env file if exists
if [ -f "$SCRIPT_DIR/.env" ]; then
    echo "Loading environment file: $SCRIPT_DIR/.env"
    set -a
    # Remove CRLF line endings for Windows compatibility
    source <(tr -d '\r' < "$SCRIPT_DIR/.env")
    set +a
elif [ -f "$SCRIPT_DIR/default.env" ]; then
    echo "Loading default environment file: $SCRIPT_DIR/default.env"
    set -a
    # Remove CRLF line endings for Windows compatibility
    source <(tr -d '\r' < "$SCRIPT_DIR/default.env")
    set +a
fi

# Default values (command line args override .env, .env overrides defaults)
REGISTRY_HOST="${REGISTRY_HOST:-localhost}"
REGISTRY_PORT="${REGISTRY_PORT:-5000}"
IMAGE_NAME="${IMAGE_NAME:-00-monolitic}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
RELEASE_NAME="${RELEASE_NAME:-monolitic-stack}"
NAMESPACE="${NAMESPACE:-default}"
DEPLOY_ACTION="${DEPLOY_ACTION:-install-or-upgrade}"
DEPLOY_MODE="${DEPLOY_MODE:-}"

# Project root directory
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHART_DIR="$PROJECT_ROOT/01-infrastructure/03-stack-monolitic"
VALUES_FILE="$SCRIPT_DIR/values.yaml"

# Build registry and repository separately
if [ -z "$REGISTRY_PORT" ] || [ "$REGISTRY_PORT" = "0" ]; then
    REGISTRY="$REGISTRY_HOST"
    IMAGE_REPOSITORY="$IMAGE_NAME"
else
    REGISTRY="$REGISTRY_HOST:$REGISTRY_PORT"
    IMAGE_REPOSITORY="$IMAGE_NAME"
fi

# Check for remote kubeconfig in same directory
REMOTE_KUBECONFIG="$SCRIPT_DIR/remote-kubeconfig.yaml"

# Validate and set deployment mode
if [ -z "$DEPLOY_MODE" ]; then
    echo "Error: DEPLOY_MODE must be set to 'remote' or 'local'"
    echo "Please set DEPLOY_MODE in .env file or as environment variable"
    exit 1
fi

if [ "$DEPLOY_MODE" != "remote" ] && [ "$DEPLOY_MODE" != "local" ]; then
    echo "Error: DEPLOY_MODE must be 'remote' or 'local'"
    exit 1
fi

# Set kubeconfig path based on mode
if [ "$DEPLOY_MODE" = "remote" ]; then
    if [ ! -f "$REMOTE_KUBECONFIG" ]; then
        echo "Error: DEPLOY_MODE=remote but remote-kubeconfig.yaml not found"
        echo "Please run ./00-fetch-kubeconfig.sh first"
        exit 1
    fi
    KUBECONFIG_PATH="$REMOTE_KUBECONFIG"
else
    KUBECONFIG_PATH=""
fi

echo "=========================================="
echo "Helm Chart Deployment"
echo "=========================================="
echo "Mode: $DEPLOY_MODE"
echo "Action: $DEPLOY_ACTION"
echo "Release: $RELEASE_NAME"
echo "Namespace: $NAMESPACE"
echo "Registry Image: $REGISTRY_IMAGE:$IMAGE_TAG"
if [ "$DEPLOY_MODE" = "remote" ]; then
    echo "Kubeconfig: $KUBECONFIG_PATH"
fi
echo "=========================================="

# Check helm
if ! command -v helm &> /dev/null; then
    echo "Error: helm not found"
    exit 1
fi

# Check chart directory
if [ ! -d "$CHART_DIR" ]; then
    echo "Error: Chart directory not found: $CHART_DIR"
    exit 1
fi

# Update helm dependencies locally
echo ""
echo "1. Updating Helm dependencies..."
cd "$CHART_DIR"
helm dependency update

# Check kubectl
if ! command -v kubectl &> /dev/null; then
    echo "Error: kubectl not found"
    exit 1
fi

# Determine action
if [ "$DEPLOY_ACTION" = "install-or-upgrade" ]; then
    # Auto-detect: check if release exists
    if [ "$DEPLOY_MODE" = "remote" ]; then
        if helm list -n "$NAMESPACE" --kubeconfig="$KUBECONFIG_PATH" | grep -q "^$RELEASE_NAME"; then
            DEPLOY_ACTION="upgrade"
        else
            DEPLOY_ACTION="install"
        fi
    else
        if helm list -n "$NAMESPACE" | grep -q "^$RELEASE_NAME"; then
            DEPLOY_ACTION="upgrade"
        else
            DEPLOY_ACTION="install"
        fi
    fi
fi

# Validate action
if [ "$DEPLOY_ACTION" != "install" ] && [ "$DEPLOY_ACTION" != "upgrade" ] && [ "$DEPLOY_ACTION" != "delete" ] && [ "$DEPLOY_ACTION" != "install-or-upgrade" ]; then
    echo "Error: DEPLOY_ACTION must be 'install', 'upgrade', 'delete', or 'install-or-upgrade'"
    exit 1
fi

if [ "$DEPLOY_ACTION" = "delete" ]; then
    # Delete release
    echo ""
    echo "2. Deleting release..."
    if [ "$DEPLOY_MODE" = "remote" ]; then
        helm uninstall "$RELEASE_NAME" --namespace "$NAMESPACE" --kubeconfig="$KUBECONFIG_PATH" || true
    else
        helm uninstall "$RELEASE_NAME" --namespace "$NAMESPACE" || true
    fi
    echo ""
    echo "Release '$RELEASE_NAME' deleted"
elif [ "$DEPLOY_MODE" = "remote" ]; then
    # Remote deployment: Use remote-kubeconfig.yaml from same directory
    echo ""
    echo "2. Deploying to remote cluster using remote-kubeconfig.yaml..."
    
    if [ "$DEPLOY_ACTION" = "install" ]; then
        echo "   Installing new release..."
        helm install "$RELEASE_NAME" "$CHART_DIR" \
            --namespace "$NAMESPACE" \
            --create-namespace \
            --kubeconfig="$KUBECONFIG_PATH" \
            --values "$VALUES_FILE" \
            --set app-monolitic.image.registry="$REGISTRY" \
            --set app-monolitic.image.repository="$IMAGE_REPOSITORY" \
            --set app-monolitic.image.tag="$IMAGE_TAG"
    else
        echo "   Upgrading existing release..."
        helm upgrade "$RELEASE_NAME" "$CHART_DIR" \
            --namespace "$NAMESPACE" \
            --kubeconfig="$KUBECONFIG_PATH" \
            --values "$VALUES_FILE" \
            --set app-monolitic.image.registry="$REGISTRY" \
            --set app-monolitic.image.repository="$IMAGE_REPOSITORY" \
            --set app-monolitic.image.tag="$IMAGE_TAG"
    fi
    
    echo ""
    echo "3. Checking deployment status..."
    kubectl get pods -n "$NAMESPACE" --kubeconfig="$KUBECONFIG_PATH" -l app.kubernetes.io/instance="$RELEASE_NAME"
else
    # Local deployment
    # Check kubeconfig
    if [ -z "$KUBECONFIG" ] && [ ! -f "$HOME/.kube/config" ]; then
        echo "Warning: KUBECONFIG not set and ~/.kube/config not found"
        echo "Please set KUBECONFIG environment variable or configure kubectl"
    fi
    
    echo ""
    echo "2. Deploying to local cluster..."
    
    if [ "$DEPLOY_ACTION" = "install" ]; then
        echo "   Installing new release..."
        helm install "$RELEASE_NAME" "$CHART_DIR" \
            --namespace "$NAMESPACE" \
            --create-namespace \
            --values "$VALUES_FILE" \
            --set app-monolitic.image.registry="$REGISTRY" \
            --set app-monolitic.image.repository="$IMAGE_REPOSITORY" \
            --set app-monolitic.image.tag="$IMAGE_TAG"
    else
        echo "   Upgrading existing release..."
        helm upgrade "$RELEASE_NAME" "$CHART_DIR" \
            --namespace "$NAMESPACE" \
            --values "$VALUES_FILE" \
            --set app-monolitic.image.registry="$REGISTRY" \
            --set app-monolitic.image.repository="$IMAGE_REPOSITORY" \
            --set app-monolitic.image.tag="$IMAGE_TAG"
    fi
    
    echo ""
    echo "3. Checking deployment status..."
    kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/instance="$RELEASE_NAME"
fi

echo ""
echo "=========================================="
echo "Done!"
echo "Release: $RELEASE_NAME"
echo "Namespace: $NAMESPACE"
echo "=========================================="

