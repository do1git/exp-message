# web-landing

r-message 랜딩 페이지 Helm Chart입니다.

## 소스

정적 파일 및 Dockerfile: `03-frontend/00-landing/`

## 이미지 빌드

```bash
cd 03-frontend/00-landing
docker build -t 00-landing:latest .
```

로컬 레지스트리(kind/minikube) 사용 시:

```bash
# kind 예시
docker tag 00-landing:latest localhost:5000/00-landing:latest
docker push localhost:5000/00-landing:latest
```
