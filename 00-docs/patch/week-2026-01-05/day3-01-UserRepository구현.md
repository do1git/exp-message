# 패치노트 - 2026-01-07

## 개요

User Repository 구현체를 JPA를 사용하여 구현하고, 도메인 서비스 테스트 코드를 작성했습니다.

---

## 구현 내용

### 1. JPA 의존성 추가

**build.gradle.kts**
- `spring-boot-starter-data-jpa` 의존성 추가
- H2 데이터베이스를 `testRuntimeOnly`로 변경 (테스트 전용)

### 2. JPA Entity 및 Repository 구현

#### 2.1 UserEntity (`user/infrastructure/UserEntity.kt`)
- 도메인 객체와 분리된 JPA Entity
- `@Entity`, `@Table` 어노테이션 사용
- 이메일 유니크 인덱스 설정 (`idx_email`)
- 필드: id, email, passwordHash, nickname, createdAt, updatedAt

#### 2.2 UserJpaRepository (`user/infrastructure/UserJpaRepository.kt`)
- Spring Data JPA Repository 인터페이스
- `JpaRepository<UserEntity, String>` 상속
- `findByEmail(email: String)` 메서드 정의

#### 2.3 UserRepositoryImpl (`user/infrastructure/UserRepositoryImpl.kt`)
- 도메인 `UserRepository` 인터페이스의 JPA 구현체
- `@Repository` 어노테이션으로 Spring Bean 등록
- Entity ↔ Domain 변환 로직 (`toEntity`, `toDomain`)
- `save`, `findById`, `findByEmail`, `delete` 메서드 구현

### 3. 테스트 코드 작성

#### 3.1 UserDomainServiceTest (`user/domain/UserDomainServiceTest.kt`)
- `@DataJpaTest`를 사용한 통합 테스트
- 테스트 케이스:
  - 사용자 생성 성공
  - 이메일 중복 시 예외 발생
  - 사용자 닉네임 업데이트 성공
  - 존재하지 않는 사용자 업데이트 시 예외 발생
  - 사용자 삭제 성공
  - 존재하지 않는 사용자 삭제 시 예외 발생

### 4. 테스트 환경 설정

#### 4.1 test/resources/application.properties
- H2 인메모리 데이터베이스 설정
- JPA/Hibernate 설정 (ddl-auto=create-drop)
- Security 자동 설정 비활성화

#### 4.2 main/resources/application.properties 정리
- 기본 설정만 유지 (애플리케이션 이름, 빈 오버라이딩 허용)
- 프로덕션 환경에서는 MySQL 등 실제 DB 설정 사용 예정

---

## 주요 설계 결정사항

### 1. 도메인 객체와 JPA Entity 분리

**이유:**
- 도메인 객체는 비즈니스 로직에 집중
- JPA Entity는 데이터베이스 매핑에 집중
- 관심사의 분리 (Separation of Concerns)

**구조:**
```
Domain Layer: User (도메인 객체)
    ↓
Infrastructure Layer: UserEntity (JPA Entity)
    ↓
Database: users 테이블
```

### 2. Repository 패턴 구현

**구조:**
- `UserRepository` (도메인 인터페이스) ← `UserRepositoryImpl` (구현체)
- `UserJpaRepository` (Spring Data JPA) ← `UserRepositoryImpl`에서 사용

**장점:**
- 도메인 레이어가 인프라 구현에 의존하지 않음
- 향후 다른 데이터 저장소로 교체 용이
- 테스트 시 Mock Repository 사용 가능

### 3. 테스트 전용 H2 데이터베이스

**이유:**
- 프로덕션과 테스트 환경 분리
- 빠른 테스트 실행 (인메모리 DB)
- 프로덕션에서는 MySQL 사용 예정

---

## 파일 구조

```
user/
├── domain/
│   ├── User.kt
│   ├── UserRepository.kt (인터페이스)
│   ├── UserDomainService.kt
│   └── ...
└── infrastructure/
    ├── UserEntity.kt (JPA Entity)
    ├── UserJpaRepository.kt (Spring Data JPA)
    └── UserRepositoryImpl.kt (구현체)

test/
├── resources/
│   └── application.properties (테스트 설정)
└── kotlin/
    └── user/
        └── domain/
            └── UserDomainServiceTest.kt
```

---

## 테스트 실행 결과

모든 테스트 케이스 통과:
- 사용자 생성/수정/삭제 기능 검증
- 이메일 중복 체크 검증
- 예외 처리 검증

