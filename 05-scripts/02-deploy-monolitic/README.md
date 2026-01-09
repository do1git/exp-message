# 모놀리틱 서버 배포 스크립트

모놀리틱 서버를 빌드, 푸시하고 Helm 차트로 배포하는 스크립트입니다.

## 구조

```
02-deploy-monolitic/
├── Chart.yaml               # Helm 차트 정의
├── templates/               # Helm 템플릿
│   ├── ingress.yaml
│   └── _helpers.tpl
├── values.yaml              # 개인 설정 파일 (Git ignore, 실제 사용)
├── values.yaml.example      # 설정 예시 파일 (Git commit, 참고용)
└── README.md
```

## Prerequisites

배포를 시작하기 전에 다음 요구사항을 확인하세요.

### 필수 도구

- Docker
- Helm 3.0+
- kubectl (원격 배포 시)

### 이미지 레지스트리

- Docker 이미지를 저장하고 배포할 레지스트리가 필요합니다
- 로컬 클러스터 사용 시: 로컬 레지스트리 (예: `localhost:5000`) 사용 가능
- 원격 클러스터 사용 시: 원격 레지스트리를 꼭 구축해야 합니다

### Kubernetes 클러스터

- **로컬 클러스터**: Docker Desktop, minikube, kind 등
- **원격 클러스터**: k3s, EKS, GKE, AKS 등
  - 원격 클러스터 사용 시 `kubeconfig.yaml` 파일이 필요합니다
  - 원격 클러스터는 이미지 레지스트리에 연결되어 있어야 합니다
- **Nginx Ingress Controller**: Ingress 리소스를 사용하기 위해 nginx ingress 플러그인이 설치되어 있어야 합니다


### 초기 설정

**values.yaml 설정:**

```bash
# values.yaml.example을 복사하여 개인 설정 파일 생성
cp values.yaml.example values.yaml
```

`values.yaml`에서 다음 항목들을 수정하세요:

- `mysql.mysql`: 데이터베이스 구축을 위한 설정
- `app-monolitic.image.registry`: 레지스트리 주소
  - 원격 레지스트리를 사용하는 경우 변경필요
- `app-monolitic.database`: 애플리케이션에서 사용할 데이터베이스 연결 설정
- `ingress.host`: Ingress 호스트 주소
  - 호스트 주소가 있는 경우에만 입력

## 사용법

### 이미지 빌드 및 푸시

`values.yaml`의 `app-monolitic.image.registry`에 설정된 레지스트리에 `00-monolitic:latest` 이미지를 빌드하고 푸시합니다.

```bash
cd ../../02-backend/00-monolitic
docker build -t 00-monolitic:latest .
docker tag 00-monolitic:latest <레지스트리>/00-monolitic:latest
docker push <레지스트리>/00-monolitic:latest
```

### Helm 차트 배포

Helm 명령어를 직접 사용하여 배포합니다.

#### 1. 처음 배포시 (Release install)

```bash
helm dependency update --kubeconfig=kubeconfig.yaml
helm install message-stack . --kubeconfig=kubeconfig.yaml --values ./values.yaml

# 배포 상태 확인
kubectl get pods --kubeconfig=kubeconfig=kubeconfig.yaml -l app.kubernetes.io/instance=message-stack
```

#### 2. 변경 내용 업데이트시 (Release upgrade)

```bash
helm dependency update --kubeconfig=kubeconfig.yaml
helm upgrade message-stack . --kubeconfig=kubeconfig.yaml --values ./values.yaml

# 배포 상태 확인
kubectl get pods --kubeconfig=kubeconfig=kubeconfig.yaml -l app.kubernetes.io/instance=message-stack
```

### 2-1. 어플리케이션 코드만 수정된 경우

```bash
# 0. 도커 레지스트리 지정
DOCKER_REGISTRY="localhost:5000"

# 1. 새 이미지 빌드 및 푸시
docker build -t 00-monolitic:latest ../../02-backend/00-monolitic
docker tag 00-monolitic:latest $DOCKER_REGISTRY/00-monolitic:latest
docker push $DOCKER_REGISTRY/00-monolitic:latest

# 2. Helm upgrade로 새 이미지 배포
helm upgrade message-stack . --kubeconfig=./kubeconfig.yaml --values ./values.yaml

# 3. 배포 상태 확인
kubectl get pods --kubeconfig=./kubeconfig.yaml -l app.kubernetes.io/instance=message-stack
```

```powershell
# 0. 도커 레지스트리 지정
$env:DOCKER_REGISTRY="localhost:5000"

# 1. 새 이미지 빌드 및 푸시
docker build -t 00-monolitic:latest ..\..\02-backend\00-monolitic
docker tag 00-monolitic:latest "$env:DOCKER_REGISTRY/00-monolitic:latest"
docker push "$env:DOCKER_REGISTRY/00-monolitic:latest"

# 2. Helm upgrade로 새 이미지 배포
helm upgrade message-stack . --kubeconfig=./kubeconfig.yaml --values ./values.yaml

# 3. 배포 상태 확인
kubectl get pods --kubeconfig=./kubeconfig.yaml -l app.kubernetes.io/instance=message-stack
```

#### 3. 배포 삭제시 (Release uninstall)

```bash
helm uninstall message-stack --kubeconfig=kubeconfig.yaml
```
