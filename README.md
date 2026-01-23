# exp-message

## 프로젝트 소개

exp-message는 여러 기술 스택을 활용하여 구축하는 **아주 튼튼한 채팅 서비스 제작** 프로젝트입니다.

이 프로젝트의 **백엔드**는  안정성, 확장성, 성능을 모두 갖춘 엔터프라이즈급 채팅 서버를 개발하는 것을 목표합니다.

이 프로젝트의 **프론트엔드**는 최소한의 기능만 구현하여 백엔드 서버의 안정성과 성능에 집중합니다. (프론트 개발에 관심 있으시면 편하게 연락 주세요.)

## 주요 기능

- **사용자 인증/인가**: JWT 기반 사용자 인증 및 권한 관리
- **채팅방 관리**: 채팅방 생성, 수정, 삭제 및 멤버 관리
- **메시지 전송/수신**: 실시간 메시지 전송 및 수신
- **메시지 검색**: Elasticsearch를 활용한 메시지 검색 `개발 예정`
- **실시간 통신**: WebSocket을 통한 실시간 메시지 전송 `개발 예정`
- **푸시 알림**: 이벤트 기반 푸시 알림 `개발 예정`

## 문서

- Swagger UI: [message.rahoon.site/api/swagger-ui.html](https://message.rahoon.site/api/swagger-ui.html)
- AsyncAPI (WebSocket): [message.rahoon.site/api/websocket-docs](https://message.rahoon.site/api/websocket-docs)
- 백엔드 개발 노트는 [00-docs/01-patch-rahoon](./00-docs/01-patch-rahoon)에서 확인하실 수 있습니다.
