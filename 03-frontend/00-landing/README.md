# r-message 랜딩 페이지

정적 HTML/CSS로 구성된 간단한 랜딩 페이지입니다.

## 실행 방법

로컬에서 확인하려면 `index.html`을 브라우저로 열거나, 간단한 HTTP 서버를 사용하세요:

```bash
# Python 3
python -m http.server 8080

# npx (Node.js)
npx serve .
```

이후 `http://localhost:8080` 접속

## 위젯 연동

`/widget.iife.js`를 로드해 채팅 위젯을 초기화합니다.
파일은 `03-frontend/01-widget`에서 `npm run build`를 실행하면 자동으로 복사됩니다.

`01-infrastructure/00-compose-all` 경로에서 `docker compose up --build`를 실행하는 경우에는
`00-landing/Dockerfile.compose`가 `01-widget`를 함께 빌드하므로 별도 수동 빌드가 필요 없습니다.

## Docker로 호스팅

```bash
# 이미지 빌드
docker build -t r-message-landing .

# 컨테이너 실행 (포트 8080)
docker run -d -p 8080:80 --name r-message-landing r-message-landing
```

이후 `http://localhost:8080` 접속
