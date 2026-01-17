# 패치노트 - Soft Delete 도입

## 문제 상황 / 결정 사항

### 1) 물리 삭제로 인한 데이터 복구 불가

- **문제 상황**: 현재 모든 삭제는 물리 삭제(실제 DB 레코드 삭제)로, 삭제된 데이터 복구가 불가능하고 감사 추적이 제한적
- **결정 사항**: **Soft Delete(논리 삭제)를 기본 삭제 전략으로 도입**

### 2) Soft Delete 구현 방식

- **문제 상황**: Soft Delete 구현 방식 선택 (JPA 어노테이션 vs 수동 처리 vs 공통 인터페이스)
- **결정 사항**: **공통 인터페이스 + 확장 함수 + Hibernate Filter (프로그래밍 방식)** 채택
  - `SoftDeletableEntity` 인터페이스: Entity에 `deletedAt` 필드 정의
  - 확장 함수: JpaRepository에 `softDeleteById()` 헬퍼 제공
  - **Hibernate Filter**: EntityManagerFactory 설정에서 프로그래밍 방식으로 필터 등록
  - **이유**: 
    - Repository 패턴과 잘 맞고, 중복 코드 최소화, 유연성 확보
    - Entity에 Hibernate 어노테이션 추가 불필요 (인프라 의존성 분리)
    - Hibernate Filter로 JPQL/Criteria 쿼리 자동 적용 (수동 수정 불필요)
    - 필요 시 필터 비활성화로 삭제된 데이터 조회 가능

### 3) 삭제된 데이터 조회 정책

- **문제 상황**: 삭제된 데이터 조회가 필요한 경우(복구, 감사 등)와 일반 조회를 구분해야 함
- **결정 사항**:
  - 기본 조회: 삭제된 데이터 제외 (`deletedAt IS NULL`)
  - 관리자/특수 조회: 삭제된 데이터 포함 옵션 제공 (필요 시)

### 4) 삭제된 데이터 정리 전략

- **문제 상황**: Soft Delete된 데이터가 계속 누적되면 저장 공간 및 성능 이슈 발생 가능
- **결정 사항**:
  - 주기적 물리 삭제 배치 작업 작성 (예: 90일 이상 삭제된 데이터)
  - 배치 작업은 별도 모듈(`01-db-migrations` 또는 새로운 배치 모듈)에서 관리

### 5) 적용 우선순위

- **문제 상황**: 모든 도메인에 한 번에 적용하면 영향 범위가 큼
- **결정 사항**: 아래 순서로 단계 적용
  - Message → ChatRoomMember → ChatRoom → User

## 테스트 계획

- [ ] 단위 테스트: Soft Delete 후 조회 시 제외되는지 검증
- [ ] 단위 테스트: 삭제된 데이터 복구 기능 검증
- [ ] 통합 테스트: Soft Delete 후 관련 데이터 조회 시 영향 검증
- [ ] 배치 테스트: 주기적 물리 삭제 배치 작업 검증

## 최종 정리(현재 합의된 스펙)

- **삭제 방식**: Soft Delete(논리 삭제) 기본, 물리 삭제는 배치 작업에서만 수행
- **구현 방식**: 공통 인터페이스 + 확장 함수 + Hibernate Filter
  - `SoftDeletableEntity` 인터페이스: Entity에 `deletedAt: LocalDateTime?` 필드 정의
  - 확장 함수: JpaRepository에 `softDeleteById()` 헬퍼 제공
  - Hibernate Filter: EntityManagerFactory 설정에서 프로그래밍 방식으로 필터 등록
  - RepositoryImpl에서 `delete()` → `softDeleteById()` 호출로 변경
  - **주의**: Hibernate Filter는 JPQL, Criteria 쿼리에만 자동 적용되며, `findById()` 같은 기본 JPA 메서드에는 적용되지 않음
- **조회 정책**
  - 기본 조회: 삭제된 데이터 제외 (`deletedAt IS NULL`)
  - 관리자 조회: 삭제된 데이터 포함 옵션 (필요 시 별도 메서드 제공)
- **정리 전략**
  - 주기적 배치 작업으로 오래된 삭제 데이터 물리 삭제 (예: 90일 이상)
- **적용 우선순위**: Message → ChatRoomMember → ChatRoom → User

## 구현 상세

### 1. 공통 인터페이스 및 확장 함수

- **SoftDeletableEntity**: Entity에 `deletedAt: LocalDateTime?` 필드를 정의하는 인터페이스
- **확장 함수**: JpaRepository에 `softDeleteById()` 헬퍼 메서드 제공

### 2. Hibernate Filter 설정

- **JpaSoftDeleteFilterMetadataContributor**: 애플리케이션 시작 시 Filter 정의 등록
  - Filter 이름: `softDeleteFilter`
  - Filter 조건: `deleted_at IS NULL`
  - `JpaEntityBase`를 상속하는 모든 Entity에 자동 적용
  
- **JpaConfig**: 트랜잭션 시작 시마다 Filter 자동 활성화
  - `JpaTransactionManager` 커스터마이징
  - 각 트랜잭션마다 `session.enableFilter("softDeleteFilter")` 호출
  
- **장점**: Entity에 Hibernate 어노테이션 추가 불필요 (인프라 의존성 분리)
- **Filter 활성화/비활성화**: 필요 시 필터 비활성화하여 삭제된 데이터 조회 가능

### 3. SoftDeleteContext 헬퍼 클래스

- **위치**: `common/domain/SoftDeleteContext.kt`
- **목적**: 필터 비활성화/활성화를 간편하게 처리하는 헬퍼 제공
- **메소드**:
  - `SoftDeleteContext.disable { }`: 필터를 비활성화한 상태에서 코드 실행 후 자동 복원
  - `SoftDeleteContext.enable { }`: 필터를 활성화한 상태에서 코드 실행 후 자동 복원
  - `SoftDeleteContext.isEnabled()`: 필터 활성화 여부 확인
  - `SoftDeleteContext.isDisabled()`: 필터 비활성화 여부 확인

### 4. 주요 변경 사항

- **Entity**: `SoftDeletableEntity` 인터페이스 구현, `deletedAt` 필드 추가 (Hibernate 어노테이션 불필요)
- **JpaConfig**: EntityManagerFactory 설정에서 Filter 프로그래밍 방식으로 등록
- **RepositoryImpl**: `delete()` → `softDeleteById()` 호출로 변경
- **JPQL/Criteria 쿼리**: Filter가 자동으로 `deletedAt IS NULL` 조건 추가 (수정 불필요)
- **기본 JPA 메서드** (`findById()` 등): Filter가 적용되지 않으므로 수동으로 `deletedAt` 체크 필요

## 피드백 (구현 후 작성)

### 잘한 점

- **SoftDeleteContext 헬퍼 클래스 추가**: `Lock.execute`, `Tx.execute`와 같은 패턴으로 필터 비활성화/활성화 헬퍼 제공하여 사용법이 간단하고 일관성 있는 패턴 제공
- **필터 상태에 따른 조건부 처리**: `findById()` 같은 기본 JPA 메서드에서 필터 활성화 여부에 따라 `deletedAt` 체크 로직 분기 처리
- **로깅 추가**: 필터 복원 실패 시 경고 로그 기록으로 디버깅 및 모니터링 용이
- **코틀린 로깅 라이브러리 사용**: `KotlinLogging`을 사용하여 lazy evaluation 및 코틀린스러운 문법 활용
- **원자적 연산 보장**: `JpaSoftDeleteRepository.softDeleteById()`에서 단일 UPDATE 쿼리로 `deletedAt` 설정 및 `deletedAt IS NULL` 조건 체크를 한 번에 처리하여 동시성 문제 방지
- **인프라 의존성 분리**: Entity에 Hibernate 어노테이션을 추가하지 않고 `AdditionalMappingContributor`를 통해 프로그래밍 방식으로 필터를 등록하여 Entity 레이어의 인프라 의존성 최소화
- **자동 필터 적용**: `JpaEntityBase`를 상속하는 모든 Entity에 자동으로 필터가 적용되어 수동 설정 불필요
- **트랜잭션별 자동 필터 활성화**: `JpaConfig`에서 커스텀 `JpaTransactionManager`를 통해 각 트랜잭션 시작 시 자동으로 필터 활성화하여 개발자가 수동으로 활성화할 필요 없음
- **MappedSuperclass 패턴**: `JpaEntityBase`를 `@MappedSuperclass`로 설계하여 상속 구조가 깔끔하고 공통 필드 관리 용이
- **안전한 필터 복원**: `SoftDeleteContext`에서 `finally` 블록을 사용하여 예외 발생 시에도 필터 상태가 원래대로 복원되도록 보장

### 아쉬운 점

- **기본 JPA 메서드 필터 미적용**: `findById()` 같은 기본 JPA 메서드는 Hibernate Filter가 적용되지 않아 수동으로 `deletedAt` 체크가 필요함. 이로 인해 개발자가 실수로 삭제된 데이터를 조회할 수 있는 위험이 있음
- **트랜잭션 의존성**: `SoftDeleteContext`의 모든 메서드가 트랜잭션 내에서만 동작하므로, 트랜잭션이 없는 컨텍스트에서는 사용 불가능
- **에러 처리 제한**: `JpaSoftDeleteRepository.softDeleteById()`에서 ID 속성을 찾지 못하거나 쿼리 실행 실패 시 예외 처리만 하고, 삭제된 엔티티를 다시 삭제하려는 경우 등에 대한 명시적인 처리 부족
- **테스트 코드 부족**: Soft Delete 기능에 대한 통합 테스트나 단위 테스트가 충분하지 않아 실제 동작 검증이 어려울 수 있음
- **성능 고려 부족**: `deletedAt` 컬럼에 대한 인덱스 추가가 "다음으로 해볼 것"에만 언급되어 있고, 실제 적용 여부가 불명확함. 대량의 데이터에서 삭제된 데이터를 제외하는 쿼리 성능에 영향을 줄 수 있음
- **복구 기능 미구현**: 삭제된 데이터 복구 기능이 "다음으로 해볼 것"에만 언급되어 있고, 실제 구현이 없어 복구가 필요한 경우 수동으로 DB 쿼리를 실행해야 함
- **배치 작업 미구현**: 오래된 삭제 데이터를 정리하는 배치 작업이 계획만 있고 구현되지 않아 삭제된 데이터가 계속 누적될 수 있음
- **Filter에 대해 깊은 이해 부족**: Hibernate Filter의 동작 원리, 적용 범위, 성능 영향 등에 대한 깊은 이해가 부족하여 구현 과정에서 예상치 못한 문제가 발생할 수 있음

## 다음으로 해볼 것

- **DB 마이그레이션 추가**: `deletedAt` 컬럼 추가 마이그레이션 작성
- **Repository 수정**: 각 도메인 Repository에 Soft Delete 로직 적용
  - 어떻게 효율적으로 중복되는 코드를 제거할지 고민해봐야함
- **복구 기능 추가**: 삭제된 데이터 복구 API (필요 시)
- **배치 작업 작성**: 주기적 물리 삭제 배치 작업 구현
- **인덱스 추가**: `deletedAt` 컬럼 인덱스 추가 (조회 성능 최적화)
- **관리자 조회 옵션**: 삭제된 데이터 포함 조회 기능 (필요 시)
