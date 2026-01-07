# How to use helm

## 설치

```bash
helm dependency update && helm install <릴리즈명> <경로>
```

## 업그레이드

```bash
helm upgrade <릴리즈명> <경로>
```

## 롤백

```bash
helm rollback <릴리즈명>
```

## 삭제

```bash
helm uninstall <릴리즈명>
```

## 로그 확인

```bash
kubectl get pod
kubectl logs -f <pod-name>
```

## 포트 포워딩

```bash
kubectl get svc
kubectl port-forward svc/msg-mono-all-local-mysql 3306:3306
```
