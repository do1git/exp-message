# Port Forward 대안 방법들

`kubectl port-forward`는 명령어이므로 YAML로 직접 지정할 수 없습니다. 하지만 영구적인 포트 포워딩을 위한 여러 대안이 있습니다.

## 방법 1: Port Forward Proxy Deployment (추천)

Helm 차트에 포함된 프록시 Deployment를 사용합니다. `socat`을 사용하여 클러스터 내부에서 포트 포워딩을 수행합니다.

### 사용 방법

```bash
# values.yaml 또는 --set으로 활성화
helm install mysql . --set service.portForward.enabled=true \
  --set service.portForward.serviceType=NodePort \
  --set service.portForward.nodePort=30306
```

또는 `values.yaml`에서:
```yaml
service:
  portForward:
    enabled: true
    serviceType: NodePort
    nodePort: 30306
```

**접속:**
```bash
mysql -h <NODE_IP> -P 30306 -u message_user -p
```

## 방법 2: Service를 NodePort/LoadBalancer로 직접 설정 (가장 간단)

MySQL 서비스 자체를 외부에 노출합니다.

```yaml
service:
  type: NodePort
  port: 3306
  nodePort: 30306
```

## 방법 3: NGINX Ingress Controller의 TCP 지원

NGINX Ingress Controller가 설치되어 있다면 TCP 서비스를 지원합니다.

### ConfigMap 생성

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: tcp-services
  namespace: ingress-nginx
data:
  "3306": default/mysql:3306
```

### Ingress Controller 설정

NGINX Ingress Controller의 ConfigMap에 TCP 서비스를 추가합니다.

## 방법 4: kubectl port-forward를 자동화하는 Deployment (비권장)

kubectl을 실행하는 Pod를 만들 수 있지만, 복잡하고 권장하지 않습니다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql-port-forward
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql-port-forward
  template:
    metadata:
      labels:
        app: mysql-port-forward
    spec:
      serviceAccountName: port-forward-sa
      containers:
      - name: kubectl
        image: bitnami/kubectl:latest
        command:
          - /bin/sh
          - -c
          - |
            while true; do
              kubectl port-forward svc/mysql 3306:3306 || sleep 5
            done
```

**주의:** 이 방법은 ServiceAccount와 적절한 권한이 필요하며, Pod가 재시작되면 연결이 끊어집니다.

## 비교표

| 방법 | 장점 | 단점 | 권장 상황 |
|------|------|------|----------|
| **Port Forward Proxy** | 영구적, 자동 재시작, YAML로 관리 | 추가 리소스 사용 | 개발/스테이징 |
| **NodePort** | 간단, 빠름 | 보안 위험, 포트 범위 제한 | 로컬 개발 |
| **LoadBalancer** | 클라우드 네이티브 | 비용, 외부 노출 | 프로덕션 (필요시) |
| **Ingress TCP** | 표준 방식 | Ingress Controller 필요 | 프로덕션 |
| **kubectl Deployment** | - | 복잡, 불안정 | 비권장 |

## 권장 사항

- **로컬 개발**: NodePort 또는 Port Forward Proxy
- **프로덕션**: ClusterIP + VPN/프록시 또는 LoadBalancer (필요시)
- **스테이징**: Port Forward Proxy 또는 NodePort

