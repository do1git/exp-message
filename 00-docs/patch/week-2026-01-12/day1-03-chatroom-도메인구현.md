# 패치노트 - 2026-01-12

## ChatRoom 도메인 구현

### 목표

채팅방 도메인을 구현하여 채팅방 생성, 조회, 수정, 삭제 기능을 제공합니다.

### 구현 내용

#### 1. 도메인 레이어 (Domain Layer)

**생성된 파일:**
- `ChatRoom.kt`: 채팅방 도메인 엔티티
  - `create()`: 새로운 채팅방 생성
  - `updateName()`: 채팅방 이름 업데이트
- `ChatRoomCommand.kt`: 도메인 명령 객체
  - `Create`: 채팅방 생성 명령
  - `Update`: 채팅방 수정 명령
  - `Delete`: 채팅방 삭제 명령
- `ChatRoomInfo.kt`: 도메인 정보 객체
  - `Detail`: 채팅방 상세 정보
- `ChatRoomError.kt`: 채팅방 관련 에러 정의
  - `CHAT_ROOM_NOT_FOUND`: 채팅방을 찾을 수 없음
  - `INVALID_NAME`: 잘못된 채팅방 이름
  - `UNAUTHORIZED_ACCESS`: 권한 없음
- `ChatRoomRepository.kt`: 채팅방 저장소 인터페이스
- `ChatRoomDomainService.kt`: 채팅방 도메인 서비스
  - `create()`: 채팅방 생성
  - `update()`: 채팅방 수정
  - `delete()`: 채팅방 삭제
  - `getById()`: ID로 채팅방 조회
  - `getByCreatedByUserId()`: 생성자 ID로 채팅방 목록 조회
- `ChatRoomCreateValidator.kt`: 채팅방 생성 검증 컴포넌트
- `ChatRoomUpdateValidator.kt`: 채팅방 수정 검증 컴포넌트

#### 2. Infrastructure 레이어

**생성된 파일:**
- `ChatRoomEntity.kt`: JPA 엔티티
  - `chat_rooms` 테이블 매핑
  - `created_by_user_id` 인덱스 설정
- `ChatRoomJpaRepository.kt`: Spring Data JPA Repository
  - `findByCreatedByUserId()`: 생성자 ID로 조회
- `ChatRoomRepositoryImpl.kt`: Repository 구현체
  - 도메인 객체와 JPA 엔티티 간 변환 로직

#### 3. Application 레이어

**생성된 파일:**
- `ChatRoomApplicationService.kt`: 애플리케이션 서비스
  - 도메인 서비스를 호출하고 결과 반환
- `ChatRoomCriteria.kt`: 애플리케이션 레이어 입력 DTO
  - `Create`: 채팅방 생성 요청
  - `Update`: 채팅방 수정 요청

#### 4. Controller 레이어

**생성된 파일:**
- `ChatRoomController.kt`: REST API 컨트롤러
  - `POST /chat-rooms`: 채팅방 생성
  - `GET /chat-rooms/{id}`: 채팅방 조회
  - `GET /chat-rooms`: 내가 생성한 채팅방 목록 조회
  - `PUT /chat-rooms/{id}`: 채팅방 수정
  - `DELETE /chat-rooms/{id}`: 채팅방 삭제
- `ChatRoomRequest.kt`: 요청 DTO
  - `Create`: 채팅방 생성 요청
  - `Update`: 채팅방 수정 요청
- `ChatRoomResponse.kt`: 응답 DTO
  - `Create`: 채팅방 생성 응답
  - `Detail`: 채팅방 상세 응답
  - `ListItem`: 채팅방 목록 항목 응답

#### 5. 테스트

**생성된 파일:**
- `ChatRoomControllerE2ETest.kt`: E2E 테스트
  - 채팅방 생성 성공/실패 테스트
  - 채팅방 조회 성공 테스트
  - 내가 생성한 채팅방 목록 조회 테스트
  - 채팅방 수정 성공 테스트
  - 채팅방 삭제 성공 테스트
  - 인증 없이 접근 시도 테스트
  - 유효성 검증 실패 테스트

### API 엔드포인트

- `POST /api/chat-rooms` - 채팅방 생성 (인증 필요)
- `GET /api/chat-rooms/{id}` - 채팅방 조회 (인증 필요)
- `GET /api/chat-rooms` - 내가 생성한 채팅방 목록 조회 (인증 필요)
- `PUT /api/chat-rooms/{id}` - 채팅방 수정 (인증 필요)
- `DELETE /api/chat-rooms/{id}` - 채팅방 삭제 (인증 필요)

### 설계 결정

1. **User 도메인과 동일한 패턴 적용**
   - Domain → Application → Controller 레이어 구조
   - Command/Criteria/Request/Response DTO 분리
   - 도메인 검증 컴포넌트 분리

2. **인증 통합**
   - 모든 API에 `@AuthInfoAffect(required = true)` 적용
   - `AuthInfo`를 통해 현재 사용자 정보 주입

3. **데이터베이스 설계**
   - `chat_rooms` 테이블 생성
   - `created_by_user_id` 인덱스 설정으로 조회 성능 최적화
   - UUID 기반 ID 사용 (마이크로서비스 분리 대비)

### 알려진 이슈 및 TODO

1. **채팅방 생성 시 생성자 자동 추가**
   - 현재 채팅방 생성 시 생성자가 멤버로 자동 추가되지 않음
   - `ChatRoomApplicationService.create()`에 TODO 주석 추가
   - ChatRoomMember 도메인 구현 후 해결 예정

2. **채팅방 수정/삭제 권한 검증**
   - 현재 생성자만 수정/삭제 가능하도록 권한 검증이 없음
   - `ChatRoomController.update()`에 TODO 주석 추가
   - ChatRoomMember 도메인 구현 후 권한 검증 로직 추가 예정

3. **채팅방 목록 조회 범위**
   - 현재는 생성한 채팅방만 조회 가능
   - 참여한 채팅방 전체 조회는 ChatRoomMember 도메인 구현 후 추가 예정

### 테스트 결과

모든 E2E 테스트 통과:
- ✅ 채팅방 생성 성공
- ✅ 채팅방 생성 실패 - 인증 없음
- ✅ 채팅방 생성 실패 - 이름 누락
- ✅ 채팅방 조회 성공
- ✅ 내가 생성한 채팅방 목록 조회 성공
- ✅ 채팅방 수정 성공
- ✅ 채팅방 삭제 성공

### AI 피드백

#### 잘한 점

1. **명확한 레이어 분리**
   - Domain, Application, Infrastructure, Controller 레이어가 명확히 분리되어 있어 유지보수성이 좋습니다.
   - 각 레이어의 책임이 명확하게 정의되어 있습니다.

2. **도메인 로직 캡슐화**
   - `ChatRoom.create()`, `ChatRoom.updateName()`처럼 비즈니스 로직이 도메인 엔티티에 잘 캡슐화되어 있습니다.
   - 불변성을 유지하면서 업데이트를 처리하는 방식이 적절합니다.

3. **검증 로직 분리**
   - `ChatRoomCreateValidator`, `ChatRoomUpdateValidator`로 검증 로직을 별도 컴포넌트로 분리하여 재사용성과 테스트 용이성을 높였습니다.

4. **DTO 계층 분리**
   - Command/Criteria/Request/Response DTO가 각 레이어별로 명확히 분리되어 있어 레이어 간 의존성이 잘 관리됩니다.

5. **포괄적인 E2E 테스트**
   - 성공 케이스뿐만 아니라 실패 케이스(인증 실패, 유효성 검증 실패)도 잘 테스트되어 있습니다.
   - Testcontainers를 활용한 실제 환경과 유사한 테스트 환경 구성이 좋습니다.

6. **일관된 인증 통합**
   - 모든 API에 `@AuthInfoAffect(required = true)`를 일관되게 적용하여 보안이 잘 관리됩니다.

7. **트랜잭션 관리**
   - `@Transactional` 어노테이션을 적절히 사용하여 데이터 일관성을 보장합니다.

8. **에러 처리**
   - `DomainException`을 통한 일관된 에러 처리 방식이 좋습니다.

#### 아쉬운 점

1. **권한 검증 부재**
   - 채팅방 수정/삭제 시 생성자인지 확인하는 권한 검증이 없습니다. 현재는 인증만 되면 누구나 수정/삭제할 수 있는 상태입니다.
   - 채팅방 조회 시에도 참여자 여부 확인 없이 누구나 조회 가능합니다.

2. **채팅방 생성 시 멤버 자동 추가 미구현**
   - 채팅방 생성 시 생성자가 멤버로 자동 추가되지 않습니다. 이는 ChatRoomMember 도메인 구현 후 해결 예정이지만, 현재 상태에서는 생성자도 자신이 만든 채팅방에 참여하지 않은 상태입니다.

3. **페이징 미지원**
   - 채팅방 목록 조회 시 페이징이 없어 대량의 데이터 조회 시 성능 문제가 발생할 수 있습니다.

4. **하드 삭제 방식**
   - 현재는 물리적 삭제를 수행합니다. 소프트 삭제(soft delete)를 고려하면 데이터 복구나 감사(audit) 목적으로 유용할 수 있습니다.

5. **테스트 케이스 보완 필요**
   - 존재하지 않는 채팅방 수정/삭제 시나리오 테스트가 없습니다.
   - 권한이 없는 사용자의 수정/삭제 시도 테스트가 없습니다.

6. **채팅방 이름 중복 검증 없음**
   - 동일한 이름의 채팅방을 여러 개 생성할 수 있습니다. 비즈니스 요구사항에 따라 중복 검증이 필요할 수 있습니다.

7. **도메인 서비스 메서드 시그니처**
   - `ChatRoomApplicationService.update()`, `delete()` 메서드에 `userId` 파라미터가 없어 권한 검증을 추가하기 어려운 구조입니다.

### 다음 단계

1. **ChatRoomMember 도메인 구현**
   - 채팅방 참가/나가기 기능
   - 채팅방 멤버 목록 조회
   - 권한 검증 로직 추가

2. **Message 도메인 구현**
   - 메시지 전송
   - 메시지 조회 (페이징)
   - 채팅방별 메시지 목록

3. **DB 마이그레이션 도입**
   - Flyway 또는 Liquibase 설정
   - `chat_rooms` 테이블 마이그레이션 작성
