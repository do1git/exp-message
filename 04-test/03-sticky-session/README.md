

**APP 인스턴스 2개 설정**
```
docker-compose up -d --scale app=2
```

**요청 전송 및 정상 작동 확인**
```
pnpm test
```

**Stikysession 작동 확인**
1. nginx 로그 확인