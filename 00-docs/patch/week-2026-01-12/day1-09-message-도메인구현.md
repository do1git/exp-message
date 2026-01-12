# 패치노트 - 2026-01-12

## Message 도메인 구현

### 목표

메시지 도메인을 구현하여 메시지 전송, 조회 기능을 제공합니다. 채팅방 멤버만 메시지를 전송할 수 있도록 권한 검증을 포함합니다.

### 구현 내용

#### 1. Domain 레이어

**생성된 파일:**
- `Message.kt`: 메시지 도메인 엔티티
  - `create()`: 새로운 메시지 생성
- `MessageCommand.kt`: 도메인 명령 객체
  - `Create`: 메시지 전송 명령
- `MessageError.kt`: 메시지 관련 에러 정의
  - `MESSAGE_NOT_FOUND`: 메시지를 찾을 수 없음
  - `INVALID_CONTENT`: 잘못된 메시지 내용
  - `UNAUTHORIZED_ACCESS`: 메시지 전송 권한 없음
  - `CHAT_ROOM_NOT_FOUND`: 채팅방을 찾을 수 없음
- `MessageRepository.kt`: 메시지 저장소 인터페이스
  - `save()`: 메시지 저장
  - `findById()`: ID로 메시지 조회
  - `findByChatRoomIdOrderByCreatedAtDesc()`: 채팅방별 메시지 목록 조회 (최신순)
- `MessageDomainService.kt`: 메시지 도메인 서비스
  - `create()`: 메시지 생성
  - `getById()`: ID로 메시지 조회
  - `getByChatRoomId()`: 채팅방별 메시지 목록 조회 (최신순)
- `MessageCreateValidator.kt`: 메시지 생성 검증 컴포넌트
  - 메시지 내용 검증 (필수, 최대 10000자)
  - 채팅방 ID, 사용자 ID 검증

#### 2. Infrastructure 레이어

**생성된 파일:**
- `MessageEntity.kt`: JPA 엔티티
  - `messages` 테이블 매핑
  - `chat_room_id`, `user_id` 인덱스 설정
  - `chat_room_id, created_at` 복합 인덱스 설정 (정렬 최적화)
- `MessageJpaRepository.kt`: Spring Data JPA Repository
  - `findByChatRoomIdOrderByCreatedAtDesc()`: 채팅방별 메시지 조회 (최신순)
- `MessageRepositoryImpl.kt`: Repository 구현체
  - 도메인 객체와 JPA 엔티티 간 변환 로직

#### 3. Application 레이어

**생성된 파일:**
- `MessageApplicationService.kt`: 애플리케이션 서비스
  - `create()`: 메시지 전송 (채팅방 존재 확인, 멤버 권한 검증)
  - `getById()`: 메시지 조회
  - `getByChatRoomId()`: 채팅방별 메시지 목록 조회 (채팅방 존재 확인)
- `MessageCriteria.kt`: 애플리케이션 레이어 입력 DTO
  - `Create`: 메시지 전송 요청
  - `GetByChatRoomId`: 채팅방별 메시지 조회 요청

#### 4. Controller 레이어

**생성된 파일:**
- `MessageController.kt`: 메시지 컨트롤러
  - `POST /messages`: 메시지 전송
  - `GET /messages/{id}`: 메시지 조회
  - `GET /messages/chat-rooms/{chatRoomId}`: 채팅방별 메시지 목록 조회 (최신순)
- `MessageRequest.kt`: 컨트롤러 요청 DTO
  - `Create`: 메시지 전송 요청
- `MessageResponse.kt`: 컨트롤러 응답 DTO
  - `Create`: 메시지 전송 응답
  - `Detail`: 메시지 조회 응답

#### 5. 테스트

**생성된 파일:**
- `MessageApplicationServiceTest.kt`: Application Service 단위 테스트
  - 메시지 전송 성공
  - 메시지 전송 실패 - 채팅방이 존재하지 않음
  - 메시지 전송 실패 - 채팅방 멤버가 아님
  - 메시지 조회 성공
  - 채팅방별 메시지 목록 조회 성공
  - 채팅방별 메시지 목록 조회 실패 - 채팅방이 존재하지 않음

### 주요 기능

1. **메시지 전송**
   - 채팅방 멤버만 메시지 전송 가능
   - 채팅방 존재 여부 확인
   - 메시지 내용 검증 (필수, 최대 10000자)

2. **메시지 조회**
   - ID로 메시지 조회
   - 채팅방별 메시지 목록 조회 (최신순 정렬, 전체 반환)

3. **권한 검증**
   - 메시지 전송 시 채팅방 멤버 여부 확인
   - `ChatRoomMemberApplicationService.isMember()` 활용

### 데이터베이스 설계

- `messages` 테이블 생성
- 인덱스 설정:
  - `idx_chat_room_id`: 채팅방별 조회 최적화
  - `idx_chat_room_created_at`: 채팅방별 최신순 정렬 최적화
  - `idx_user_id`: 사용자별 조회 최적화

### API 엔드포인트

1. **POST /messages**
   - 메시지 전송
   - 요청: `{ "chatRoomId": "...", "content": "..." }`
   - 응답: `{ "id": "...", "chatRoomId": "...", "userId": "...", "content": "...", "createdAt": "..." }`

2. **GET /messages/{id}**
   - 메시지 조회
   - 응답: `{ "id": "...", "chatRoomId": "...", "userId": "...", "content": "...", "createdAt": "..." }`

3. **GET /messages/chat-rooms/{chatRoomId}**
   - 채팅방별 메시지 목록 조회 (최신순)
   - 응답: 메시지 목록 (전체 반환)

### 설계 결정

1. **도메인 객체 직접 사용**: `MessageInfo` 래퍼 객체 없이 `Message` 도메인 객체를 직접 사용하여 단순화
2. **정렬 기준**: 기본적으로 최신 메시지 순 (`createdAt DESC`)
3. **권한 검증**: Application 레이어에서 채팅방 멤버 여부 확인
4. **에러 처리**: 채팅방이 존재하지 않을 경우 `CHAT_ROOM_NOT_FOUND` 에러 반환
5. **전체 반환**: 현재는 페이징 없이 채팅방의 모든 메시지를 반환 (향후 필요 시 페이징 추가 가능)

### 성능 고려사항

1. **인덱스 최적화**
   - `chat_room_id, created_at` 복합 인덱스로 정렬 쿼리 최적화
   - 채팅방별 조회 성능 향상

### 향후 계획

1. **메시지 수정/삭제 기능**
   - 메시지 작성자만 수정/삭제 가능하도록 구현
   - 소프트 삭제(soft delete) 고려

2. **메시지 검색 기능**
   - Elasticsearch를 활용한 메시지 검색
   - 채팅방별, 사용자별, 키워드 검색

3. **실시간 메시지 전송**
   - WebSocket을 통한 실시간 메시지 전송
   - 메시지 전송 시 채팅방 멤버들에게 실시간 알림

4. **메시지 읽음 처리**
   - 메시지 읽음 상태 관리
   - 읽지 않은 메시지 수 조회

5. **메시지 타입 확장**
   - 텍스트 외 이미지, 파일 등 다양한 메시지 타입 지원

### AI 피드백

#### 잘한 점

1. **일관된 아키텍처 패턴**
   - ChatRoom, ChatRoomMember 도메인과 동일한 레이어 구조와 패턴 적용
   - Command/Criteria/Request/Response DTO 분리로 레이어 간 의존성 관리 우수

2. **권한 검증**
   - 메시지 전송 시 채팅방 멤버 여부 확인으로 보안 강화
   - Application 레이어에서 비즈니스 규칙 적용

3. **도메인 객체 직접 사용**
   - `MessageInfo` 래퍼 객체 제거로 코드 단순화
   - 불필요한 변환 로직 제거

4. **인덱스 최적화**
   - 복합 인덱스로 정렬 쿼리 성능 최적화
   - 채팅방별 조회 성능 향상

5. **포괄적인 테스트**
   - 성공 케이스뿐만 아니라 실패 케이스도 잘 테스트됨
   - Mock을 활용한 단위 테스트로 빠른 피드백

#### 아쉬운 점

1. **메시지 수정/삭제 기능 부재**
   - 현재는 메시지 전송과 조회만 가능
   - 메시지 수정/삭제 기능 추가 필요

2. **메시지 읽음 처리 미구현**
   - 메시지 읽음 상태 관리 기능 없음
   - 읽지 않은 메시지 수 조회 불가

3. **실시간 통신 미구현**
   - 현재는 REST API만 제공
   - WebSocket을 통한 실시간 메시지 전송 필요

4. **메시지 타입 제한**
   - 현재는 텍스트 메시지만 지원
   - 이미지, 파일 등 다양한 메시지 타입 지원 필요

5. **메시지 검색 기능 부재**
   - 메시지 검색 기능 없음
   - Elasticsearch를 활용한 검색 기능 추가 필요

6. **페이징 미지원**
   - 현재는 채팅방의 모든 메시지를 반환
   - 메시지가 많을 경우 성능 이슈 가능성 (향후 페이징 추가 필요)

### 다음 단계

1. **메시지 수정/삭제 기능 구현**
   - 메시지 작성자만 수정/삭제 가능하도록 구현
   - 소프트 삭제(soft delete) 고려

2. **실시간 메시지 전송 (WebSocket)**
   - WebSocket 설정 및 구현
   - 메시지 전송 시 채팅방 멤버들에게 실시간 알림

3. **메시지 읽음 처리**
   - 메시지 읽음 상태 관리
   - 읽지 않은 메시지 수 조회

4. **DB 마이그레이션 도입**
   - Flyway 또는 Liquibase 설정
   - `messages` 테이블 마이그레이션 작성

5. **메시지 검색 기능**
   - Elasticsearch 설정 및 연동
   - 메시지 인덱싱 및 검색 API 구현

6. **페이징 기능 추가**
   - 메시지가 많을 경우를 대비한 페이징 기능 고려
   - Spring Data의 `Pageable` 활용
