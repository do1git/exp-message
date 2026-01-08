# 패치노트 - 2026-01-07

## 목표

Docker Compose를 사용하여 MySQL 데이터베이스를 로컬 개발 환경에 구성하고, Kubernetes를 사용하여 MySQL 데이터베이스를 클러스터 환경에 배포합니다. 또한 binlog를 설정하여 추후 특정 시점으로 데이터베이스를 복구할 수 있도록 구성합니다.

### 목표 설정 이유

1. 추후 DB를 서비스에 따라 분리하기 위함
2. 추후 DB를 클러스터로 관리하기 위함
3. 데이터 손실 시 특정 시점으로 복구할 수 있는 백업/복구 전략 수립

---

## 구현 내용

### 1단계: Docker Compose로 로컬 MySQL 환경 구성

#### 1.1 docker-compose.yml 작성
- MySQL 8.0 이미지 사용
- binlog 활성화 설정
- 데이터 영속성을 위한 볼륨 마운트
- 환경 변수 설정 (root 비밀번호, 데이터베이스명 등)
- 포트 매핑 (3306:3306)

#### 1.2 MySQL binlog 설정
- `my.cnf` 또는 환경 변수를 통한 binlog 활성화
- `log-bin` 설정
- `binlog_format=ROW` 설정 (ROW 기반 복제 및 복구)
- `expire_logs_days` 설정 (binlog 보관 기간)

#### 1.3 로컬 환경 테스트
- Docker Compose로 MySQL 컨테이너 실행
- binlog 활성화 확인
- 데이터베이스 연결 테스트

### 2단계: Kubernetes 매니페스트 작성

#### 2.1 ConfigMap 생성
- MySQL 설정 파일 (`my.cnf`) 포함
- binlog 설정 포함

#### 2.2 Secret 생성
- MySQL root 비밀번호
- 데이터베이스 사용자 정보

#### 2.3 PersistentVolumeClaim 생성
- MySQL 데이터 영속성을 위한 PVC
- StorageClass 설정

#### 2.4 Deployment 생성
- MySQL 컨테이너 정의
- ConfigMap, Secret, PVC 마운트
- 환경 변수 설정
- 리소스 제한 설정 (CPU, Memory)

#### 2.5 Service 생성
- ClusterIP 타입으로 MySQL 서비스 노출
- 포트 3306 매핑

### 3단계: Kubernetes 배포 및 검증

#### 3.1 Kubernetes 클러스터에 배포
- kubectl을 사용한 리소스 배포
- 배포 상태 확인

#### 3.2 MySQL 연결 테스트
- Pod 내부에서 MySQL 클라이언트로 연결 테스트
- 외부에서 포트 포워딩을 통한 연결 테스트

#### 3.3 binlog 동작 확인
- 데이터 변경 후 binlog 파일 생성 확인
- binlog 내용 조회 (`mysqlbinlog` 명령어)

### 4단계: 특정 시점 복구 테스트

#### 4.1 테스트 데이터 생성
- 샘플 데이터 삽입
- binlog 위치 기록

#### 4.2 데이터 삭제 시뮬레이션
- 테스트 데이터 삭제

#### 4.3 binlog를 이용한 복구
- binlog를 사용하여 특정 시점으로 복구
- 복구 결과 검증
