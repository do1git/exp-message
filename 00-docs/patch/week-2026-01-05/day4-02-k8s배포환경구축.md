# 패치노트 - 2026-01-08

## 목표했던 내용: Kubernetes 배포 환경 구축

- 실제 서버에 Kubernetes 클러스터 구축
- Docker Desktop Kubernetes 에러 해결 또는 다른 배포 환경 구축 중 선택 필요

## 변경사항

### 기술 스택 결정

- **k3s 선택**: kubeadm 대신 k3s로 결정
  - 이유: 빠른 구축, 단일 서버에 적합, 리소스 효율적, 프로덕션 사용 가능
  - kubeadm은 멀티 노드/프로덕션 환경에 적합하지만 복잡도 높음

### 테스트 환경 구성

- **Proxmox + Ubuntu VM**으로 테스트 환경 구축
- **VM 사양 결정**:
  - CPU: 4 cores
  - RAM: 4GB
  - 디스크: 40GB (Thin provisioning 권장)
  - 리소스 분석:
    - k3s: 200-300MB RAM
    - MySQL: 256-512MB RAM, 10GB 디스크
    - Spring Boot App: 512MB-1GB RAM
    - Ingress Controller: 100-200MB RAM
    - 시스템 오버헤드: 500MB-1GB RAM

### 다음 단계

- Proxmox에 Ubuntu VM 생성
- k3s 설치 및 구성
- Helm 차트 배포 테스트
