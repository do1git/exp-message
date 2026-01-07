# MSG Mono All Local Umbrella Chart

모든 서비스(MySQL, Redis, Application 등)를 단일 Helm 릴리스로 배포하는 Umbrella 차트입니다.

## 설치

```bash
helm dependency update && helm install msg-mono-all-local .
```

## 업그레이드

```bash
helm upgrade msg-mono-all-local .
```

## 롤백

```bash
helm rollback msg-mono-all-local
```

## 삭제

```bash
helm uninstall msg-mono-all-local
```

## 서비스 확인

```bash
kubectl get svc
```

## 로그 확인

```bash
kubectl logs -f <pod-name>
```

## 포트 포워딩

```bash
kubectl port-forward svc/msg-mono-all-local-mysql 3306:3306
```
