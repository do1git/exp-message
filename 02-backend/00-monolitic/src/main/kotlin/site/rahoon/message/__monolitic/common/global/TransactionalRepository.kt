package site.rahoon.message.__monolitic.common.global

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass

/**
 * Repository 인터페이스나 구현체에 트랜잭션을 자동으로 적용하는 메타 어노테이션
 *
 * @Repository와 @Transactional을 결합한 어노테이션으로,
 * enableDefaultTransactions = false 설정으로 인해 기본 트랜잭션이 비활성화된 경우
 * 리포지토리에 명시적으로 트랜잭션을 적용하기 위해 사용합니다.
 *
 * @param transactionManager 트랜잭션 매니저 빈 이름 (기본값: "")
 * @param propagation 트랜잭션 전파 방식 (기본값: Propagation.REQUIRED)
 * @param isolation 트랜잭션 격리 수준 (기본값: Isolation.DEFAULT)
 * @param timeout 트랜잭션 타임아웃 (초, 기본값: -1)
 * @param readOnly 읽기 전용 여부 (기본값: true)
 * @param rollbackFor 롤백할 예외 클래스 (기본값: {})
 * @param rollbackForClassName 롤백할 예외 클래스 이름 (기본값: {})
 * @param noRollbackFor 롤백하지 않을 예외 클래스 (기본값: {})
 * @param noRollbackForClassName 롤백하지 않을 예외 클래스 이름 (기본값: {})
 *
 * @example
 * <pre>
 * `interface MyRepository {
 * fun save(entity: Entity): Entity
 * }
 *
 * = true)
 * class ReadOnlyRepositoryImpl : MyRepository {
 * // 읽기 전용 트랜잭션이 적용됨
 * }
 *
 * = Isolation.SERIALIZABLE)
 * class SerializableRepositoryImpl : MyRepository {
 * // SERIALIZABLE 격리 수준이 적용됨
 * }
` *
</pre> *
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repository
@Transactional
annotation class TransactionalRepository(
    @get:AliasFor(annotation = Transactional::class, attribute = "transactionManager")
    val transactionManager: String = "",

    @get:AliasFor(annotation = Transactional::class, attribute = "propagation")
    val propagation: Propagation = Propagation.REQUIRED,

    @get:AliasFor(annotation = Transactional::class, attribute = "isolation")
    val isolation: Isolation = Isolation.DEFAULT,

    @get:AliasFor(annotation = Transactional::class, attribute = "timeout")
    val timeout: Int = -1,

    // 기본적으로 클래스에만 사용하기 때문에 true로 지정함
    @get:AliasFor(annotation = Transactional::class, attribute = "readOnly")
    val readOnly: Boolean = true,

    @get:AliasFor(annotation = Transactional::class, attribute = "rollbackFor")
    val rollbackFor: Array<KClass<out Throwable>> = [],

    @get:AliasFor(annotation = Transactional::class, attribute = "rollbackForClassName")
    val rollbackForClassName: Array<String> = [],

    @get:AliasFor(annotation = Transactional::class, attribute = "noRollbackFor")
    val noRollbackFor: Array<KClass<out Throwable>> = [],

    @get:AliasFor(annotation = Transactional::class, attribute = "noRollbackForClassName")
    val noRollbackForClassName: Array<String> = []
)
