package site.rahoon.message.__monolitic.common.domain

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.hibernate.Session
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * JPA SoftDelete 필터 관리를 위한 헬퍼 클래스
 * SoftDelete 필터를 비활성화한 상태에서 코드 실행을 간소화하는 유틸리티 메소드 제공
 */
@Component
class SoftDeleteContext(
    private val entityManagerFactory: EntityManagerFactory
) {
    private val logger = KotlinLogging.logger {}
    companion object {
        @Volatile
        lateinit var inst: SoftDeleteContext
            private set

        /**
         * SoftDelete 필터를 비활성화한 상태에서 action을 실행합니다.
         * action 실행 후 필터는 자동으로 다시 활성화됩니다.
         *
         * @param action 필터가 비활성화된 상태에서 실행할 작업
         * @return 작업 실행 결과
         */
        fun <T> disable(
            action: () -> T
        ): T {
            return inst.disable(action)
        }

        /**
         * SoftDelete 필터를 활성화한 상태에서 action을 실행합니다.
         * action 실행 후 필터는 자동으로 원래 상태로 복원됩니다.
         *
         * @param action 필터가 활성화된 상태에서 실행할 작업
         * @return 작업 실행 결과
         */
        fun <T> enable(
            action: () -> T
        ): T {
            return inst.enable(action)
        }

        /**
         * SoftDelete 필터가 현재 활성화되어 있는지 확인합니다.
         *
         * @return 필터가 활성화되어 있으면 true, 그렇지 않으면 false
         */
        fun isEnabled(): Boolean {
            return inst.isEnabled()
        }

        /**
         * SoftDelete 필터가 현재 비활성화되어 있는지 확인합니다.
         *
         * @return 필터가 비활성화되어 있으면 true, 그렇지 않으면 false
         */
        fun isDisabled(): Boolean {
            return inst.isDisabled()
        }
    }

    @PostConstruct
    fun init() {
        inst = this
    }

    /**
     * SoftDelete 필터를 비활성화한 상태에서 action을 실행합니다.
     * action 실행 후 필터는 자동으로 다시 활성화됩니다.
     *
     * @param action 필터가 비활성화된 상태에서 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> disable(
        action: () -> T
    ): T {
        val filterName = "softDeleteFilter"
        val entityManager = getCurrentEntityManager()
            ?: throw IllegalStateException("현재 활성화된 트랜잭션이 없습니다. 트랜잭션 내에서 호출해야 합니다.")

        val session = try {
            entityManager.unwrap(Session::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Hibernate Session을 가져올 수 없습니다. Hibernate를 사용하는지 확인하세요.", e)
        }

        val wasEnabled = session.getEnabledFilter(filterName) != null

        return try {
            if (wasEnabled) {
                session.disableFilter(filterName)
            }
            action()
        } finally {
            if (wasEnabled) {
                try {
                    session.enableFilter(filterName)
                } catch (e: Exception) {
                    // 필터 재활성화 실패는 로깅만 하고 예외를 던지지 않음
                    // (이미 트랜잭션이 종료되었을 수 있음)
                    logger.warn(e) { "SoftDelete 필터 재활성화 실패: filterName=$filterName" }
                }
            }
        }
    }

    /**
     * SoftDelete 필터를 활성화한 상태에서 action을 실행합니다.
     * action 실행 후 필터는 자동으로 원래 상태로 복원됩니다.
     *
     * @param action 필터가 활성화된 상태에서 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> enable(
        action: () -> T
    ): T {
        val filterName = "softDeleteFilter"
        val entityManager = getCurrentEntityManager()
            ?: throw IllegalStateException("현재 활성화된 트랜잭션이 없습니다. 트랜잭션 내에서 호출해야 합니다.")

        val session = try {
            entityManager.unwrap(Session::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Hibernate Session을 가져올 수 없습니다. Hibernate를 사용하는지 확인하세요.", e)
        }

        val wasEnabled = session.getEnabledFilter(filterName) != null

        return try {
            if (!wasEnabled) {
                session.enableFilter(filterName)
            }
            action()
        } finally {
            if (!wasEnabled) {
                try {
                    session.disableFilter(filterName)
                } catch (e: Exception) {
                    // 필터 재비활성화 실패는 로깅만 하고 예외를 던지지 않음
                    // (이미 트랜잭션이 종료되었을 수 있음)
                    logger.warn(e) { "SoftDelete 필터 재비활성화 실패: filterName=$filterName" }
                }
            }
        }
    }

    /**
     * SoftDelete 필터가 현재 활성화되어 있는지 확인합니다.
     *
     * @return 필터가 활성화되어 있으면 true, 그렇지 않으면 false
     */
    fun isEnabled(): Boolean {
        val filterName = "softDeleteFilter"
        val entityManager = getCurrentEntityManager() ?: return false

        val session = try {
            entityManager.unwrap(Session::class.java)
        } catch (e: Exception) {
            return false
            // throw IllegalStateException("Hibernate Session을 가져올 수 없습니다. Hibernate를 사용하는지 확인하세요.", e)
        }

        return session.getEnabledFilter(filterName) != null
    }

    /**
     * SoftDelete 필터가 현재 비활성화되어 있는지 확인합니다.
     *
     * @return 필터가 비활성화되어 있으면 true, 그렇지 않으면 false
     */
    fun isDisabled(): Boolean {
        return !isEnabled()
    }

    /**
     * 현재 트랜잭션에 바인딩된 EntityManager를 가져옵니다.
     */
    private fun getCurrentEntityManager(): EntityManager? {
        val entityManagerHolder = TransactionSynchronizationManager
            .getResource(entityManagerFactory) as? EntityManagerHolder
        return entityManagerHolder?.entityManager
    }
}
