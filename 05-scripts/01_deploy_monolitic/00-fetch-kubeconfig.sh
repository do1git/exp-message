#!/bin/bash

# Fetch kubeconfig from remote server

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Default values
REMOTE_HOST=""
REMOTE_USER=""
REMOTE_KUBECONFIG_PATH=""
OUTPUT_PATH="$SCRIPT_DIR/remote-kubeconfig.yaml"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--host)
            REMOTE_HOST="$2"
            shift 2
            ;;
        -u|--user)
            REMOTE_USER="$2"
            shift 2
            ;;
        -k|--kubeconfig)
            REMOTE_KUBECONFIG_PATH="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_PATH="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -h, --host HOST           Remote server host (required)"
            echo "  -u, --user USER           Remote server user (required)"
            echo "  -k, --kubeconfig PATH     Remote kubeconfig path (optional)"
            echo "  -o, --output PATH         Output path (default: ./remote-kubeconfig.yaml)"
            echo "  --help                    Show this help message"
            echo ""
            echo "Example:"
            echo "  $0 --host 192.168.1.100 --user rahoon"
            echo "  $0 -h 192.168.1.100 -u rahoon -k /etc/rancher/k3s/k3s.yaml"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo "=========================================="
echo "Fetch Kubeconfig from Remote Server"
echo "=========================================="

# Check required variables
if [ -z "$REMOTE_HOST" ] || [ -z "$REMOTE_USER" ]; then
    echo "Error: REMOTE_HOST and REMOTE_USER must be provided"
    echo ""
    echo "Usage:"
    echo "  $0 --host HOST --user USER"
    echo "  $0 -h HOST -u USER"
    echo ""
    echo "Use --help for more information"
    exit 1
fi

echo "Remote Host: $REMOTE_USER@$REMOTE_HOST"
echo "Output Path: $OUTPUT_PATH"
echo "=========================================="

# Check SSH connection
echo ""
echo "1. Checking SSH connection..."
if ! ssh -o ConnectTimeout=5 "$REMOTE_USER@$REMOTE_HOST" exit 2>/dev/null; then
    echo "Error: Cannot connect to remote server $REMOTE_USER@$REMOTE_HOST"
    exit 1
fi
echo "   ✓ SSH connection successful"

# Determine remote kubeconfig path
if [ -z "$REMOTE_KUBECONFIG_PATH" ]; then
    echo ""
    echo "2. Detecting kubeconfig path on remote server..."
    
    # Try common paths
    if ssh "$REMOTE_USER@$REMOTE_HOST" "test -f ~/.kube/config" 2>/dev/null; then
        REMOTE_KUBECONFIG_PATH="~/.kube/config"
        echo "   Found: ~/.kube/config"
    elif ssh "$REMOTE_USER@$REMOTE_HOST" "sudo test -f /etc/rancher/k3s/k3s.yaml" 2>/dev/null; then
        REMOTE_KUBECONFIG_PATH="/etc/rancher/k3s/k3s.yaml"
        echo "   Found: /etc/rancher/k3s/k3s.yaml (requires sudo)"
    else
        echo "Error: Could not find kubeconfig on remote server"
        echo "Please specify path with --kubeconfig option"
        exit 1
    fi
else
    echo ""
    echo "2. Using specified kubeconfig path: $REMOTE_KUBECONFIG_PATH"
fi

# Fetch kubeconfig
echo ""
echo "3. Fetching kubeconfig..."
if [[ "$REMOTE_KUBECONFIG_PATH" == *"/etc/rancher/k3s/k3s.yaml"* ]]; then
    # Use sudo for k3s.yaml
    ssh "$REMOTE_USER@$REMOTE_HOST" "sudo cat $REMOTE_KUBECONFIG_PATH" > "$OUTPUT_PATH"
else
    # Regular file
    scp "$REMOTE_USER@$REMOTE_HOST:$REMOTE_KUBECONFIG_PATH" "$OUTPUT_PATH"
fi

# Update server address if needed
if [ -n "$REMOTE_HOST" ]; then
    echo "   Updating server address to $REMOTE_HOST..."
    # Replace localhost/127.0.0.1 with actual host
    sed -i.bak "s|server:.*127.0.0.1.*|server: https://$REMOTE_HOST:6443|" "$OUTPUT_PATH" 2>/dev/null || \
    sed -i '' "s|server:.*127.0.0.1.*|server: https://$REMOTE_HOST:6443|" "$OUTPUT_PATH" 2>/dev/null || \
    sed -i "s|server:.*localhost.*|server: https://$REMOTE_HOST:6443|" "$OUTPUT_PATH" 2>/dev/null || true
    rm -f "$OUTPUT_PATH.bak" 2>/dev/null || true
fi

# Set permissions
chmod 600 "$OUTPUT_PATH"

echo ""
echo "=========================================="
echo "Done!"
echo "Kubeconfig saved to: $OUTPUT_PATH"
echo ""
echo "To use this kubeconfig:"
echo "  export KUBECONFIG=$OUTPUT_PATH"
echo "  or set LOCAL_KUBECONFIG=$OUTPUT_PATH in .env"
echo ""
echo "Note: remote-kubeconfig.yaml is in .gitignore"
echo "=========================================="

