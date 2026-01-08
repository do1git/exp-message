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

- **기존 Ubuntu VM 사용 결정**
- **현재 VM 사양**:
  - CPU: 4 cores (권장과 동일)
  - RAM: 16GB (권장 4GB보다 여유 있음, 현재 1.69GB 사용 중)
  - 디스크: 128GB (권장 40GB보다 여유 있음)
  - 상태: running
- **사양 분석**:
  - 권장 사양(4 cores, 4GB RAM, 40GB 디스크) 충족
  - 리소스 여유가 많아 확장 가능
  - 기존 VM 재사용으로 효율적

### k3s, Helm 설치 및 확인

**k3s 설치 (Traefik 비활성화):**

```bash
# Traefik을 비활성화하고 설치 (80/443 포트 충돌 방지)
curl -sfL https://get.k3s.io | sh -s - --disable traefik

# 현재 사용자가 sudo 없이 kubectl 사용하도록 설정
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
chmod 600 ~/.kube/config

# 확인
kubectl get nodes
```

**Helm 설치:**

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

### Docker 레지스트리 설정

**로컬 Docker 레지스트리 실행:**

```bash
docker run -d -p 5000:5000 --name registry \
  --restart=always \
  -v /var/lib/registry:/var/lib/registry \
  registry:2
```

**k3s에서 레지스트리 사용 설정:**

```bash
# k3s 레지스트리 설정 파일 생성
sudo mkdir -p /etc/rancher/k3s
sudo cat > /etc/rancher/k3s/registries.yaml <<EOF
mirrors:
  localhost:5000:
    endpoint:
      - "http://localhost:5000"
EOF

# k3s 재시작 (설정 적용)
sudo systemctl restart k3s
```

**레지스트리 확인:**

```bash
# 레지스트리 동작 확인
curl http://localhost:5000/v2/_catalog

# hello-world 이미지 테스트
docker pull hello-world
docker tag hello-world:latest localhost:5000/hello-world:latest
docker push localhost:5000/hello-world:latest

# Kubernetes에 서비스로 배포
kubectl create deployment hello-world --image=localhost:5000/hello-world:latest
kubectl expose deployment hello-world --type=NodePort --port=8080
kubectl get pods,svc

# 삭제
kubectl delete service hello-world
kubectl delete deployment hello-world
```

### 로컬 개발 환경에서 이미지 배포하기

**필요한 작업 리스트:**

1. 레지스트리를 외부에서 접근 가능하도록 설정
   - 방화벽 포트 오픈 (5000)
   - 레지스트리 컨테이너를 외부 IP로 바인딩 (0.0.0.0:5000)

2. 로컬 개발 환경에서 이미지 빌드
   - Dockerfile로 이미지 빌드
   - 서버 IP로 태그 지정

3. 레지스트리에 이미지 푸시
   - 로컬에서 서버 레지스트리로 푸시

4. k3s에서 외부 레지스트리 접근 설정
   - registries.yaml에 서버 IP 추가
   - insecure registry 설정

5. Helm 차트 배포
   - Helm 차트로 애플리케이션 배포
