# 패치노트 - Soft Delete 보일러플레이트 제거 방안

## Before

- `TestRepositoryImpl`에 소프트 삭제가 구현되어 있음
- 다른 리포지터리들(`UserRepositoryImpl`, `ChatRoomRepositoryImpl`, `MessageRepositoryImpl`, `ChatRoomMemberRepositoryImpl` 등)에도 동일한 패턴을 적용해야 함
- 각 리포지터리마다 반복되는 보일러플레이트 코드가 발생할 예정

## Goal

- SoftDelete로 인해 발생한 보일러 플레이트 findByID와 DeleteByID를 없앨 예정
- 그와 동시에 SoftDelete가 적용되지 않은 로직은 영향이 최소화 되어야함

## KeyDecision

- JpaSimpleRepository를 직접 구현하여 softDeleteById 제공 및 findById 오버라이딩

## Impact

- 코드 중복제거로 인한 개발 편의

---

## Goal 상세

각 레포지터리마다 아래 코드가 반복될 것으로 예상됨

```kotlin
    // FindById는 Hibernate Filter의 영샹을 받지 않음
    fun findById(id: ID): T? {
        return jpaRepository.findById(id).orElse(null)
            ?.takeIf {
                if(!(it is JpaEntityBase)) true
                else SoftDeleteContext.isDisabled() || it.deletedAt == null
            .toDoamin()
    }

    // Delete 시 delete메소드가 아닌 update로 처리해야함
    @Transactional
    override fun deleteById(id: ID, deletedAt: LocalDateTime): Int{
        val entityName = domainClass.simpleName
        return entityManager.createQuery("UPDATE $entityName e SET e.deletedAt = :now WHERE e.id = :id")
            .setParameter("now", LocalDateTime.now())
            .setParameter("id", id)
            .executeUpdate()
    }
```

## Key Decision 상세

아래와 같은 방안들을 고민해봄

- SoftDelete를 편하게 해주는 컴포넌트를 만들어볼까?
  - 😓 table명 조회를 위해 인자로 ClassType을 직접 주입해줘야하는 단점이 있음
  - 😓 해당 컴포넌트도 주입하여 선언해야하는 보일러 플레이트 코드 발생

- @SQLDelete를 지정해볼까?
  - 👍 추가적인 의존성 필요 x
  - 👍 해당 객체의 모든 삭제 요청에 대해 소프트 딜리트를 처리해줌
  - 😓 하드 딜리트를 위해서는 직접 쿼리를 날려야함
  - 😓 각 Entity마다 테이블명을 입력한 삭제 쿼리를 작성해줘야함 (테이블명 지정 실수발생가능)
  - 😓 DeleteAt을 서버에서 지정할 수 없음 (DB 설정을 따라가게됨)
  - 😓 findById는 보일러 플레이트가 여전히 남아있음

- JpaRepository 주입체를 직접 구현해볼까?
  - 👍 쿼리 반복작성 불필요, 테이블 명을 쉽게 가져올 수 있음
  - 👍 추가적인 의존성 필요 x
  - 👍 기존 JPA 메소드 활용하여 메소드 작성 가능
  - 👍 삭제 시간에 대해 직접 주입 가능
  - 😓 내가 직접 처리하지 않은 메소드는 어떻게 작동할지 모름..

따라서 `보일러 플레이트를 줄이고` `비 침습적으로 적용`한다는 목표에 맞추어<br/>
**JpaRepository 주입체 직접 구현**을 선택함

## 작업 내용

1. SimpleJpaRepository를 상속한 JpaSoftDeleteRepository를 만들어 findById를 변경하고 softDelete를 추가함

2. 해당 클래스가 JpaRepository에 자동 주입되도록 설정을 추가함

3. testRepositoryImpl을 수정하고 정상 작동을 확인함

### 1. JpaSoftDeleteRepository

- 아래와 같이 SoftDelete를 위한 Repository를 추가함
- findByIdOrNull은 코틀린에서 Optional을 없애고 편하게 쓰기위해 추가함

```kotlin
@NoRepositoryBean
interface JpaSoftDeleteRepository<T, ID : Any> : JpaRepository<T, ID> {
    override fun findById(id:ID):Optional<T>
    fun findByIdOrNull(id:ID): T?
    fun softDeleteById(id: ID, deletedAt: LocalDateTime): Int
}


class JpaSoftDeleteRepositoryImpl<T , ID : Any>(
    private val entityInformation: JpaEntityInformation<T, ID>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), JpaSoftDeleteRepository<T, ID> {


    override fun findById(id: ID): Optional<T> {
        return super.findById(id).orElse(null)
            ?.takeIf {
                if(!(it is JpaEntityBase)) true
                else SoftDeleteContext.isDisabled() || it.deletedAt == null
            }.let { Optional.ofNullable(it) as Optional<T> }
    }

    override fun findByIdOrNull(id: ID): T? {
        return findById(id).orElse(null)
    }

    @Transactional
    override fun softDeleteById(id: ID, deletedAt: LocalDateTime): Int{
        val entityName = domainClass.simpleName
        return entityManager.createQuery(
            "UPDATE $entityName e SET e.deletedAt = :now WHERE e.id = :id"
        )
            .setParameter("now", LocalDateTime.now())
            .setParameter("id", id)
            .executeUpdate()
    }
}
```

### 2. JpaSoftDeleteRepository 자동 주입

- 아래와 같이 JpaSoftDeleteRepositoryImpl을 기본 레포지터리 구현체로 지정

```Kotlin
@EnableJpaRepositories(repositoryBaseClass = JpaSoftDeleteRepositoryImpl::class)
class JpaConfig
```

### 3. TestRepositoryImpl 수정

- 아래와같이 쓸모없는 보일러플레이트를 최소화 할 수 있었습니다!

```kotlin
    override fun findById(id: String): TestDomain? {
        return testJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    @Transactional
    override fun deleteById(id:String){
        testJpaRepository.softDeleteById(id, LocalDateTime.now())
    }
```

## Impact

각 구현체마다 구현해야하는 코드의 양이 아래와 같이 줄어듦

### Before

```kotlin
    // FindById는 Hibernate Filter의 영샹을 받지 않음
    fun findById(id: ID): T? {
        return jpaRepository.findById(id).orElse(null)
            ?.takeIf {
                if(!(it is JpaEntityBase)) true
                else SoftDeleteContext.isDisabled() || it.deletedAt == null
            .toDoamin()
    }

    // Delete 시 delete메소드가 아닌 update로 처리해야함
    @Transactional
    override fun deleteById(id: ID, deletedAt: LocalDateTime): Int{
        val entityName = domainClass.simpleName
        return entityManager.createQuery("UPDATE $entityName e SET e.deletedAt = :now WHERE e.id = :id")
            .setParameter("now", LocalDateTime.now())
            .setParameter("id", id)
            .executeUpdate()
    }
```

### After

```kotlin
    override fun findById(id: String): TestDomain? {
        return testJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    @Transactional
    override fun deleteById(id:String){
        testJpaRepository.softDeleteById(id, LocalDateTime.now())
    }
```
