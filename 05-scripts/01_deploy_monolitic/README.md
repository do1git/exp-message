# 모놀리틱 서버 배포 스크립트

모놀리틱 서버를 빌드, 푸시하고 Helm 차트로 배포하는 스크립트입니다.

## 스크립트

- `00-fetch-kubeconfig.sh`: 원격 서버에서 kubeconfig 가져오기
- `01-build-push-image.sh` / `01-build-push-image.bat`: Docker 이미지 빌드 및 레지스트리 푸시
- `02-deploy-helm-chart.sh` / `02-deploy-helm-chart.bat`: Helm 차트 배포

## 사용법

### 이미지 빌드 및 푸시

**Linux/Mac:**
```bash
chmod +x 01-build-push-image.sh
./01-build-push-image.sh
```

**Windows:**
```cmd
01-build-push-image.bat
```

### 원격 서버에서 kubeconfig 가져오기

**Linux/Mac:**
```bash
chmod +x 00-fetch-kubeconfig.sh
./00-fetch-kubeconfig.sh --host HOST --user USER
```

**사용법:**
```bash
# 기본 사용
./00-fetch-kubeconfig.sh --host 192.168.1.100 --user rahoon

# 짧은 옵션
./00-fetch-kubeconfig.sh -h 192.168.1.100 -u rahoon

# kubeconfig 경로 지정
./00-fetch-kubeconfig.sh -h 192.168.1.100 -u rahoon -k /etc/rancher/k3s/k3s.yaml

# 출력 경로 지정
./00-fetch-kubeconfig.sh -h 192.168.1.100 -u rahoon -o ~/my-kubeconfig.yaml

# 도움말
./00-fetch-kubeconfig.sh --help
```

### Helm 차트 배포

**Linux/Mac:**
```bash
chmod +x 02-deploy-helm-chart.sh
./02-deploy-helm-chart.sh
```

**Windows:**
```cmd
02-deploy-helm-chart.bat
```

**전체 배포 프로세스:**
```bash
# 1. 원격 서버에서 kubeconfig 가져오기 (최초 1회)
./00-fetch-kubeconfig.sh --host 192.168.1.100 --user rahoon

# 2. 이미지 빌드 및 푸시
./01-build-push-image.sh

# 3. Helm 차트 배포
./02-deploy-helm-chart.sh
```

## 환경 변수 설정

스크립트는 다음 순서로 환경 변수를 로드합니다:
1. `.env` 파일 (존재하는 경우)
2. `default.env` 파일 (존재하는 경우)
3. 환경 변수 또는 기본값

**설정 방법:**
```bash
# default.env를 복사하여 .env 생성
cp default.env .env

# .env 파일 수정
# REGISTRY_HOST=192.168.1.100
# REGISTRY_PORT=5000
# IMAGE_NAME=00-monolitic
# IMAGE_TAG=latest
```

## 환경 변수

### 공통 변수
- `REGISTRY_HOST`: 레지스트리 호스트 (기본값: localhost)
- `REGISTRY_PORT`: 레지스트리 포트 (기본값: 5000)
- `IMAGE_NAME`: 이미지 이름 (기본값: 00-monolitic)
- `IMAGE_TAG`: 이미지 태그 (기본값: latest)

### 배포 변수 (02-deploy-helm-chart.sh/bat)
- `RELEASE_NAME`: Helm release 이름 (기본값: monolitic-stack)
- `NAMESPACE`: Kubernetes namespace (기본값: default)
- `DEPLOY_MODE`: 배포 모드 (필수)
  - `remote`: 원격 클러스터에 배포 (remote-kubeconfig.yaml 필요)
  - `local`: 로컬 클러스터에 배포 (기본 kubeconfig 사용)
- `DEPLOY_ACTION`: 배포 액션 (기본값: install-or-upgrade)
  - `install-or-upgrade`: 자동 감지 (release가 있으면 upgrade, 없으면 install)
  - `install`: 새로 설치
  - `upgrade`: 업그레이드
  - `delete`: 삭제

## 예시

### 로컬 배포
```bash
# 서버 IP로 푸시 및 배포
REGISTRY_HOST=192.168.1.100 ./01-build-push-image.sh
REGISTRY_HOST=192.168.1.100 ./02-deploy-helm-chart.sh
```

### 원격 배포

**원격 배포:**
```bash
# 1. 원격 서버에서 kubeconfig 가져오기 (한 번만)
./00-fetch-kubeconfig.sh --host 192.168.1.100 --user rahoon

# 2. 이미지 빌드 및 푸시
./01-build-push-image.sh

# 3. Helm 차트 배포 (같은 폴더의 remote-kubeconfig.yaml 자동 사용)
./02-deploy-helm-chart.sh
```

**로컬 배포:**
```bash
# remote-kubeconfig.yaml이 없으면 자동으로 로컬 클러스터에 배포
./01-build-push-image.sh
./02-deploy-helm-chart.sh
```

**배포 모드 및 액션 지정:**
```bash
# 원격 클러스터에 설치 또는 업그레이드 (자동 감지)
DEPLOY_MODE=remote ./02-deploy-helm-chart.sh

# 원격 클러스터에 강제로 설치
DEPLOY_MODE=remote DEPLOY_ACTION=install ./02-deploy-helm-chart.sh

# 로컬 클러스터에 강제로 업그레이드
DEPLOY_MODE=local DEPLOY_ACTION=upgrade ./02-deploy-helm-chart.sh

# 원격 클러스터에서 삭제
DEPLOY_MODE=remote DEPLOY_ACTION=delete ./02-deploy-helm-chart.sh
```

### 커스텀 태그 사용
```bash
IMAGE_TAG=v1.0.0 ./01-build-push-image.sh
IMAGE_TAG=v1.0.0 ./02-deploy-helm-chart.sh
```

## 사전 요구사항

- Docker
- kubectl (원격 서버 kubeconfig 설정 필요)
- Helm 3.0+
- Kubernetes 클러스터 (k3s 등)

