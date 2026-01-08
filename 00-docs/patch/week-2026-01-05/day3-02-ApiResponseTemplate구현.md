# 패치노트 - 2026-01-07

## 목표

공통 API 응답 템플릿 및 전역 예외 처리 시스템을 구현하여 일관된 API 응답 구조를 제공합니다.

---

## 구현 내용

### 1. 공통 API 응답 템플릿 (`ApiResponse`)

#### 1.1 ApiResponse 구조 (`common/controller/ApiResponse.kt`)
- `success`: Boolean 플래그로 성공/실패 구분
- `data`: 성공 시 반환할 데이터 (제네릭 타입)
- `error`: 실패 시 에러 정보 (`ErrorInfo`)

#### 1.2 ErrorInfo 구조
- `code`: 에러 코드 (예: "USER_001", "VALIDATION_ERROR")
- `message`: 에러 메시지
- `details`: 추가 상세 정보 (Map<String, Any>?)
- `occurredAt`: 에러 발생 시각 (ZonedDateTime)
- `path`: 요청 경로

#### 1.3 팩토리 메서드
- `ApiResponse.success(data)`: 성공 응답 생성
- `ApiResponse.error(...)`: 에러 응답 생성

### 2. 전역 예외 핸들러 (`GlobalExceptionHandler`)

#### 2.1 DomainException 처리
- `ErrorType`에 따라 HTTP 상태 코드 자동 매핑
  - `NOT_FOUND` → 404
  - `CONFLICT` → 409
  - `SERVER_ERROR` → 500
  - `CLIENT_ERROR` → 400
- `occurredAt`과 `path` 자동 포함

#### 2.2 ApplicationException 처리
- Application 레이어 예외를 400 Bad Request로 처리

#### 2.3 Validation 예외 처리
- `MethodArgumentNotValidException` 처리
- 필드별 검증 오류를 `details`에 포함
- 에러 코드: "VALIDATION_ERROR"

#### 2.4 기타 예외 처리
- 예상치 못한 예외를 500 Internal Server Error로 처리
- 에러 코드: "INTERNAL_SERVER_ERROR"

### 3. ZonedDateTime 직렬화 설정

#### 3.1 JacksonConfig (`common/global/config/JacksonConfig.kt`)
- `ZonedDateTimeSerializer`를 전역으로 등록
- ISO-8601 형식 (`ISO_OFFSET_DATE_TIME`)으로 직렬화
- 예: `2026-01-07T13:52:33.479+09:00`

#### 3.2 ZonedDateTimeSerializer
- `ZonedDateTime`을 ISO-8601 형식의 String으로 변환
- `JacksonConfig` 내부에 private 클래스로 구현

### 4. 공통 에러 정의 (`CommonError`)

#### 4.1 CommonError (`common/domain/CommonError.kt`)
- 특정 도메인에 종속되지 않는 공통 에러 정의
- `NOT_FOUND`, `CONFLICT`, `CLIENT_ERROR`, `SERVER_ERROR` 포함
- 각 에러는 `ErrorType`과 연결되어 HTTP 상태 코드 매핑

### 5. Health Check API 및 E2E 테스트

#### 5.1 HealthController (`common/controller/HealthController.kt`)
- `/api/health`: 서비스 상태 확인
- `/api/health/error`: 다양한 에러 타입 테스트용 엔드포인트

#### 5.2 HealthControllerE2ETest
- 실제 HTTP 요청을 통한 E2E 테스트
- `occurredAt`이 ISO-8601 형식으로 직렬화되는지 검증
- `ZonedDateTime.parse()`를 사용하여 파싱 가능 여부 확인
- 다양한 에러 타입 (CLIENT_ERROR, NOT_FOUND, CONFLICT) 테스트

---

## 주요 설계 결정사항

### 1. success 플래그 기반 응답 구조
- **이유**: 프론트엔드에서 성공/실패를 명확하게 구분할 수 있도록 함
- **장점**: 타입 안전성 향상, 클라이언트 측 처리 로직 단순화

### 2. ErrorType 기반 HTTP 상태 코드 매핑
- **이유**: 도메인 레이어가 HTTP에 직접 의존하지 않도록 하면서도, 컨트롤러 레이어에서 적절한 HTTP 상태 코드를 반환
- **장점**: 도메인 로직의 순수성 유지, HTTP 상태 코드 일관성 보장

### 3. ZonedDateTime 사용 및 ISO-8601 직렬화
- **이유**: 타임존 정보를 포함한 정확한 시각 표현, 국제 표준 형식 사용
- **장점**: 다양한 클라이언트 환경에서 일관된 파싱 가능, 타임존 정보 보존

### 4. 전역 예외 핸들러를 통한 일관된 에러 응답
- **이유**: 모든 예외를 일관된 형식으로 처리하여 API 응답의 일관성 보장
- **장점**: 클라이언트 측 에러 처리 로직 단순화, 디버깅 용이성 향상

### 5. CommonError 도입
- **이유**: 특정 도메인에 종속되지 않는 공통 에러를 정의하여 재사용성 향상
- **장점**: Health Check API 등 도메인 독립적인 API에서도 일관된 에러 처리 가능

---

## 영향받는 코드

### 신규 생성
- `common/controller/ApiResponse.kt`: 공통 API 응답 템플릿
- `common/controller/GlobalExceptionHandler.kt`: 전역 예외 핸들러
- `common/domain/CommonError.kt`: 공통 에러 정의
- `common/controller/HealthController.kt`: Health Check API
- `common/global/config/JacksonConfig.kt`: Jackson 직렬화 설정
- `common/controller/HealthControllerE2ETest.kt`: E2E 테스트

### 수정
- `common/domain/DomainError.kt`: `ErrorType` enum 추가
- `user/domain/UserError.kt`: 각 에러에 `ErrorType` 할당
- `user/controller/UserController.kt`: `ApiResponse` 사용

### 삭제
- `user/controller/UserRequest.kt`: `UserCriteria`로 대체
- `user/application/UserResponse.kt`: 도메인 객체 직접 사용

---

## 테스트 결과

- Health Check API E2E 테스트 통과
- `occurredAt` ISO-8601 직렬화 검증 통과
- 다양한 에러 타입 응답 검증 통과

