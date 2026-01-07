# Everything Umbrella Chart

Umbrella chart that deploys all services (MySQL, Redis, Application, etc.) in a single Helm release.

## Overview

This chart manages multiple subcharts as dependencies:
- **mysql**: MySQL database
- **redis**: Redis cache (to be added)
- **application**: Main application (to be added)

## Installation

### Generate Values from .env

First, generate `values.yaml` from `.env` file:

**Bash (Linux/Mac):**
```bash
cd helm/everything
chmod +x generate-values.sh
./generate-values.sh
```

**PowerShell (Windows):**
```powershell
cd helm/everything
.\generate-values.ps1
```

**Custom .env file:**
```bash
./generate-values.sh ../../custom.env values-custom.yaml
```

### Default Installation (ClusterIP - 클러스터 내부 접근만)
```bash
helm dependency update
helm install everything .
```
**참고:** 기본값은 `ClusterIP`로 설정되어 있어 클러스터 내부에서만 접근 가능합니다.

### Development Environment (NodePort - 외부 접속 허용)
```bash
helm dependency update
helm install everything . -f values-dev.yaml
```
**접속 방법:**
```bash
# 노드 IP와 NodePort로 접속
mysql -h <NODE_IP> -P 30306 -u message_user -p
```

**HostPath 사용 (로컬 개발용):**
```bash
helm install everything . -f values-dev.yaml \
  --set mysql.persistence.hostPath="/data/mysql"
```

### Production Environment
```bash
helm dependency update
helm install everything . -f values-prod.yaml \
  --namespace production \
  --create-namespace \
  --set mysql.mysql.rootPassword=your-secure-password \
  --set mysql.mysql.password=your-secure-password
```

**프로덕션 환경 옵션:**

1. **LoadBalancer 사용 (클라우드 환경):**
   - `values-prod.yaml`에서 `service.type: LoadBalancer` 사용
   - 클라우드 제공업체가 외부 IP를 자동 할당
   ```bash
   # 외부 IP 확인
   kubectl get svc -n production
   ```

2. **ClusterIP 사용 (가장 안전, 권장):**
   - `values-prod.yaml`에서 `service.type: ClusterIP`로 변경
   - 클러스터 내부에서만 접근 가능
   - 외부 접속이 필요한 경우 VPN/프록시 또는 `kubectl port-forward` 사용

**Note:** 
- 프로덕션에서는 비밀번호를 `--set` 또는 Secret 관리 도구로 설정하세요.
- 환경별 .env 파일(dev.env, prod.env)을 생성하여 values 파일을 생성할 수 있습니다.

### Install with Custom Namespace
```bash
helm install everything . \
  --namespace production \
  --create-namespace \
  -f values-prod.yaml
```

## Upgrade

```bash
# Update dependencies first
helm dependency update

# Upgrade the release
helm upgrade everything .
```

## Rollback

```bash
# View release history
helm history everything

# Rollback to previous version
helm rollback everything

# Rollback to specific revision
helm rollback everything 1
```

## Uninstall

```bash
helm uninstall everything
```

## Managing Subcharts

### Enable/Disable Subcharts

You can enable or disable individual subcharts:

```bash
# Disable MySQL
helm install everything . --set mysql.enabled=false

# Enable only MySQL
helm install everything . --set mysql.enabled=true --set redis.enabled=false
```

### Override Subchart Values

Override values for specific subcharts:

```bash
# Override MySQL storage size
helm install everything . --set mysql.persistence.size=20Gi

# Override MySQL resources
helm install everything . \
  --set mysql.resources.requests.memory=512Mi \
  --set mysql.resources.limits.memory=1Gi
```

## External Access to MySQL

### Why not Ingress?

**Ingress는 HTTP/HTTPS 트래픽용입니다.** MySQL은 TCP 프로토콜을 사용하므로 Ingress를 사용할 수 없습니다.

### External Access Options

MySQL을 외부에서 접속하는 방법:

#### 1. NodePort (로컬 개발용)

노드의 특정 포트로 MySQL을 노출합니다:

```bash
# NodePort로 변경
helm install everything . --set mysql.service.type=NodePort

# 특정 포트 지정 (30000-32767 범위)
helm install everything . \
  --set mysql.service.type=NodePort \
  --set mysql.service.nodePort=30306
```

**접속 방법:**
```bash
# 노드 IP와 포트로 접속
mysql -h <NODE_IP> -P 30306 -u message_user -p
```

#### 2. LoadBalancer (클라우드 환경)

클라우드 환경에서 외부 IP를 제공합니다:

```bash
helm install everything . --set mysql.service.type=LoadBalancer
```

**접속 방법:**
```bash
# 외부 IP 확인
kubectl get svc mysql

# 외부 IP로 접속
mysql -h <EXTERNAL_IP> -P 3306 -u message_user -p
```

#### 3. kubectl Port Forwarding (임시/디버깅용)

가장 간단한 방법 (프로덕션 비권장):

```bash
# 포트 포워딩
kubectl port-forward svc/mysql 3306:3306

# 다른 터미널에서 접속
mysql -h localhost -P 3306 -u message_user -p
```

**주의:** 이 방법은 터미널이 종료되면 연결이 끊어집니다.

#### 4. Ingress Controller TCP Support (고급)

일부 Ingress Controller (예: NGINX Ingress)는 TCP/UDP를 지원합니다:

```yaml
# ConfigMap 예시 (NGINX Ingress)
apiVersion: v1
kind: ConfigMap
metadata:
  name: tcp-services
  namespace: ingress-nginx
data:
  "3306": default/mysql:3306
```

**보안 주의사항:**
- 프로덕션에서는 외부 노출을 최소화하세요
- VPN 또는 Private Network 사용 권장
- SSL/TLS 암호화 사용 권장
- 방화벽 규칙 설정

## PersistentVolume Storage

### Where is PVC data stored?

By default, PVC data is stored according to your Kubernetes cluster's StorageClass:
- **Cloud providers**: EBS volumes (AWS), Persistent Disks (GCP), etc.
- **Local clusters**: Depends on StorageClass (e.g., local-path-provisioner, hostpath-provisioner)

### Using HostPath for Local Development

For local development, you can mount data directly to a host directory:

```bash
# Option 1: Using --set
helm install everything . --set mysql.persistence.hostPath="/data/mysql"

# Option 2: Edit values.yaml
# Set mysql.persistence.hostPath: "/data/mysql"

# Option 3: Create values-local.yaml
cat > values-local.yaml <<EOF
mysql:
  persistence:
    hostPath: "/data/mysql"
EOF
helm install everything . -f values-local.yaml
```

**Important Notes:**
- HostPath only works on single-node clusters (like minikube, kind, k3s)
- The directory will be created automatically if it doesn't exist
- Make sure the path is accessible and has proper permissions
- For production, use StorageClass instead of HostPath

**Windows Example:**
```powershell
helm install everything . --set mysql.persistence.hostPath="C:\data\mysql"
```

**Linux/Mac Example:**
```bash
helm install everything . --set mysql.persistence.hostPath="/data/mysql"
```

## Adding New Services

To add a new service (e.g., Redis):

1. Create the subchart in `../redis/`
2. Add dependency to `Chart.yaml`:
   ```yaml
   dependencies:
     - name: redis
       version: 0.1.0
       repository: "file://../redis"
       condition: redis.enabled
   ```
3. Add configuration to `values.yaml`:
   ```yaml
   redis:
     enabled: true
     # Redis configuration here
   ```
4. Run `helm dependency update`
5. Install/upgrade the chart:
   ```bash
   helm install everything . -f values-dev.yaml
   ```

## Values Management

### Generating values.yaml from .env

The `values.yaml` file is generated from `.env` file using the provided scripts:
- `generate-values.sh` (Bash)
- `generate-values.ps1` (PowerShell)

This ensures consistency between Docker Compose and Helm configurations.

### Values Structure

### Global Values
- `global.namespace`: Namespace for all resources
- `global.labels`: Common labels for all resources
- `global.annotations`: Common annotations for all resources

### Subchart Values
Each subchart has its own section in values.yaml:
- `mysql.*`: MySQL configuration (generated from .env)
- `redis.*`: Redis configuration (when added)
- `application.*`: Application configuration (when added)

## Examples

### Install with custom MySQL password
```bash
helm install everything . \
  --set mysql.mysql.rootPassword=my-secure-password \
  --set mysql.mysql.password=my-secure-password
```

### Install with multiple value files
```bash
helm install everything . \
  -f values-prod.yaml \
  -f custom-overrides.yaml
```

### Install specific services only
```bash
helm install everything . \
  --set mysql.enabled=true \
  --set redis.enabled=false \
  --set application.enabled=false
```
