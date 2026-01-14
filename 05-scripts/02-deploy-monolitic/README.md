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
├── docker-build-n-push.sh   # 이미지 빌드/푸시 스크립트 (Linux/Mac)
├── docker-build-n-push.ps1  # 이미지 빌드/푸시 스크립트 (Windows)
├── helm-deploy.sh           # Helm 배포 스크립트 (Linux/Mac)
├── helm-deploy.ps1          # Helm 배포 스크립트 (Windows)
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
- `batch-db-migration.image.registry`: 마이그레이션 이미지 레지스트리 주소
- `batch-db-migration.database`: 마이그레이션에서 사용할 데이터베이스 연결 설정
- `app-monolitic.image.registry`: 애플리케이션 이미지 레지스트리 주소
  - 원격 레지스트리를 사용하는 경우 변경필요
- `app-monolitic.database`: 애플리케이션에서 사용할 데이터베이스 연결 설정
- `ingress.host`: Ingress 호스트 주소
  - 호스트 주소가 있는 경우에만 입력

## 사용법

### 이미지 빌드 및 푸시

`values.yaml`의 이미지 설정을 읽어 자동으로 빌드/푸시하는 스크립트를 제공합니다.

#### 스크립트 사용 (권장)

```bash
# Linux/Mac
./docker-build-n-push.sh

# 도움말
./docker-build-n-push.sh --help
```

```powershell
# Windows
.\docker-build-n-push.ps1

# 도움말
.\docker-build-n-push.ps1 -Help
```

스크립트는 `values.yaml`에서 다음 설정을 읽어 각 이미지를 빌드/푸시합니다:
- `app-monolitic.image.registry/repository:tag` → `00-monolitic` 이미지
- `batch-db-migration.image.registry/repository:tag` → `01-db-migrations` 이미지

### Helm 차트 배포

Helm 배포를 위한 스크립트를 제공합니다.

#### 스크립트 사용법

```bash
# Linux/Mac
./helm-deploy.sh           # 기본값: upgrade
./helm-deploy.sh <action>

# 도움말
./helm-deploy.sh --help
```

```powershell
# Windows
.\helm-deploy.ps1           # 기본값: upgrade
.\helm-deploy.ps1 <action>

# 도움말
.\helm-deploy.ps1 -Help
```

#### 액션 종류

| 액션 | 단축키 | 설명 |
|------|--------|------|
| `install` | `c` | 새로운 Release 설치 |
| `upgrade` | `u` | 기존 Release 업그레이드 (Pod 롤아웃 포함) **[기본값]** |
| `uninstall` | `d` | Release 삭제 |
| `logs-app` | `la` | App (00-monolitic) 로그 보기 |
| `logs-migration` | `lm` | Migration (01-db-migrations) 로그 보기 |

#### 1. 처음 배포시 (Release install)

```bash
./helm-deploy.sh install
# 또는
./helm-deploy.sh c
```

```powershell
.\helm-deploy.ps1 install
# 또는
.\helm-deploy.ps1 c
```

#### 2. 변경 내용 업데이트시 (Release upgrade)

```bash
./helm-deploy.sh            # 기본값
# 또는
./helm-deploy.sh u
```

```powershell
.\helm-deploy.ps1            # 기본값
# 또는
.\helm-deploy.ps1 u
```

> **참고**: upgrade 액션은 자동으로 Pod 롤아웃 재시작을 수행합니다. (latest 태그 사용 시 필요)

### 2-1. 어플리케이션 코드만 수정된 경우

```bash
# 1. 새 이미지 빌드 및 푸시
./docker-build-n-push.sh

# 2. Helm upgrade로 새 이미지 배포 (롤아웃 포함)
./helm-deploy.sh
```

```powershell
# 1. 새 이미지 빌드 및 푸시
.\docker-build-n-push.ps1

# 2. Helm upgrade로 새 이미지 배포 (롤아웃 포함)
.\helm-deploy.ps1
```

#### 3. 배포 삭제시 (Release uninstall)

```bash
./helm-deploy.sh uninstall
# 또는
./helm-deploy.sh d
```

```powershell
.\helm-deploy.ps1 uninstall
# 또는
.\helm-deploy.ps1 d
```

#### 4. 로그 보기

```bash
# App 로그
./helm-deploy.sh la

# Migration 로그
./helm-deploy.sh lm
```

```powershell
# App 로그
.\helm-deploy.ps1 la

# Migration 로그
.\helm-deploy.ps1 lm
```
