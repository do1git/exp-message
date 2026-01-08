# 패치노트 - 2026-01-06

## 개요

User Context 도메인 레이어와 공통 인프라 구조를 구현했습니다.

---

## 구현 내용

### 1. User Domain 구현

#### 1.1 User 엔티티 (`user/domain/User.kt`)
- **필드**: id (UUID String), email, passwordHash, nickname, createdAt, updatedAt
- **팩토리 메서드**: `User.create(email, passwordHash, nickname)` - User 생성 로직을 도메인 객체에 포함
- **업데이트 메서드**: `updateNickname(newNickname)` - 불변성 유지, `copy` 사용

#### 1.2 UserRepository Interface (`user/domain/UserRepository.kt`)
- `save(user: User)`: 저장/수정
- `findById(id: String)`: ID로 조회
- `findByEmail(email: String)`: Email로 조회 (UNIQUE 인덱스 활용)
- `delete(id: String)`: 삭제

#### 1.3 UserCommand (`user/domain/UserCommand.kt`)
Sealed class로 명령 객체 정의:
- `Create`: 회원가입 (email, password: raw password, nickname)
- `Update`: 사용자 정보 수정 (id, nickname)
- `Delete`: 사용자 삭제 (id)

#### 1.4 UserDomainService (`user/domain/UserDomainService.kt`)
- `create(command: UserCommand.Create)`: 이메일 중복 체크 → 비밀번호 해싱 → 사용자 생성
- `update(command: UserCommand.Update)`: 사용자 조회 → 닉네임 업데이트
- `delete(command: UserCommand.Delete)`: 사용자 조회 → 삭제
- 모든 메서드는 `@Transactional`로 트랜잭션 처리 (기본값: READ_COMMITTED)

#### 1.5 UserError (`user/domain/UserError.kt`)
DomainError 인터페이스를 구현한 에러 enum:
- `USER_NOT_FOUND`: 사용자를 찾을 수 없음
- `EMAIL_ALREADY_EXISTS`: 이메일 중복
- `INVALID_EMAIL`: 잘못된 이메일 형식
- `INVALID_PASSWORD`: 잘못된 비밀번호
- `INVALID_NICKNAME`: 잘못된 닉네임

#### 1.6 UserPasswordHasher (`user/domain/component/UserPasswordHasher.kt`)
- **인터페이스**: `UserPasswordHasher`
  - `hash(rawPassword: String)`: 원본 비밀번호를 해시로 변환
  - `verify(rawPassword: String, hashedPassword: String)`: 비밀번호 검증 (향후 로그인 등에 사용)
- **구현체**: `BCryptUserPasswordHasher` - BCrypt 알고리즘 사용, Spring Bean으로 등록

---

### 2. 공통 인프라 구현

#### 2.1 예외 처리 구조

**DomainException** (`common/domain/DomainException.kt`)
- 도메인 레벨 예외
- `error: DomainError`: 에러 enum 타입
- `details: Map<String, Any>?`: 상세 정보 (필드명과 값)
- 도메인 서비스에서만 사용

**DomainError** (`common/domain/DomainError.kt`)
- 모든 도메인 에러 enum이 구현하는 인터페이스
- `code: String`, `message: String` 속성

**ApplicationException** (`common/application/ApplicationException.kt`)
- 어플리케이션 레벨 예외
- 유효성 검증, 권한 등에서 사용

#### 2.2 트랜잭션 관리

**Tx** (`common/utils/Tx.kt`)
- 트랜잭션 실행을 위한 유틸리티 클래스 (Spring Bean)
- `Tx.execute { }`: 트랜잭션 실행 (기본값: READ_COMMITTED)
- 파라미터로 `isolationLevel`, `propagation`, `timeout`, `readOnly` 커스터마이징 가능
- Companion object의 `inst`를 통해 접근 (자기 클래스 호출 시에도 동작)
- `@PostConstruct`로 초기화하여 companion object에 인스턴스 설정

**TransactionConfig** (`common/utils/TransactionConfig.kt`)
- `@Transactional` 어노테이션의 기본 isolation level을 READ_COMMITTED로 설정
- `isolation` 속성을 명시하지 않으면 자동으로 READ_COMMITTED 적용
- 명시적으로 다른 isolation을 지정하면 해당 값 사용

---

## 주요 설계 결정사항

### 1. Rich Domain Model
- 비즈니스 로직을 도메인 객체에 포함 (`User.create()`, `updateNickname()`)
- Domain Service는 Repository 접근이 필요한 경우에만 사용

### 2. 불변성(Immutability) 유지
- 도메인 객체 업데이트 시 `copy` 사용
- **장점**: 부작용 방지, 스레드 안전성, 예측 가능한 동작, DDD 원칙 준수
- **단점**: 메모리 사용량 증가, 성능 오버헤드 (하지만 Kotlin data class의 `copy`는 효율적)

### 3. 트랜잭션 격리 수준 기본값: READ_COMMITTED
- **이유**: 중복 체크와 생성 사이에 다른 트랜잭션의 커밋된 변경사항을 확인 가능
- **예**: 이메일 중복 체크 시 최신 커밋된 데이터를 확인하여 중복 생성 방지
- **구현**: `TransactionConfig`를 통해 `@Transactional`의 기본값 설정

### 4. 비밀번호 해싱 분리
- Command는 raw password를 받음
- Domain Service에서 `UserPasswordHasher`를 통해 해싱
- 도메인 객체는 해싱된 passwordHash만 받음

### 5. 에러 처리 구조화
- `DomainException`에 `DomainError` enum과 `details` Map 전달
- 에러 코드와 메시지를 enum으로 관리하여 일관성 확보

---

## 트랜잭션 사용 예시

```kotlin
// @Transactional 사용 (기본값: READ_COMMITTED)
@Transactional
fun create(command: UserCommand.Create): User {
    // ...
}

// 명시적으로 다른 isolation 지정
@Transactional(isolation = Isolation.SERIALIZABLE)
fun someMethod() {
    // ...
}

// Tx 유틸리티 사용
Tx.inst.execute {
    // 트랜잭션 코드
}
```

---

## 파일 구조

```
user/
  domain/
    User.kt
    UserRepository.kt
    UserCommand.kt
    UserDomainService.kt
    UserError.kt
    component/
      UserPasswordHasher.kt

common/
  domain/
    DomainException.kt
    DomainError.kt
  application/
    ApplicationException.kt
  utils/
    Tx.kt
    TransactionConfig.kt
```
