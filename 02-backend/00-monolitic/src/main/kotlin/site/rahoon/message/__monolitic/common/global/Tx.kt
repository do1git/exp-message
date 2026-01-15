package site.rahoon.message.__monolitic.common.global

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

/**
 * 트랜잭션 관리를 위한 헬퍼 클래스
 * 트랜잭션 범위에서 코드 실행을 간소화하는 유틸리티 메소드 제공
 */
@Component
class Tx(private val transactionManager: PlatformTransactionManager) {
    companion object {
        @Volatile
        lateinit var inst: Tx
            private set

        fun <T> execute(
            isolationLevel: Int = TransactionDefinition.ISOLATION_READ_COMMITTED,
            propagation: Int = TransactionDefinition.PROPAGATION_REQUIRED,
            timeout: Int = TransactionDefinition.TIMEOUT_DEFAULT,
            readOnly: Boolean = false,
            action: () -> T
        ): T {
            return inst.execute(isolationLevel, propagation, timeout, readOnly, action)
        }
    }

    @PostConstruct
    fun init() {
        inst = this
    }

    /**
     * 트랜잭션 설정으로 코드 블록을 실행합니다.
     *
     * @param isolationLevel 트랜잭션 격리 수준 (기본값: READ COMMITTED)
     * @param propagation 트랜잭션 전파 방식 (기본값: REQUIRED)
     * @param timeout 트랜잭션 타임아웃 (기본값: 기본 설정 사용)
     * @param readOnly 읽기 전용 여부 (기본값: false)
     * @param action 트랜잭션 내에서 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> execute(
        isolationLevel: Int = TransactionDefinition.ISOLATION_READ_COMMITTED,
        propagation: Int = TransactionDefinition.PROPAGATION_REQUIRED,
        timeout: Int = TransactionDefinition.TIMEOUT_DEFAULT,
        readOnly: Boolean = false,
        action: () -> T
    ): T {
        val transactionTemplate = TransactionTemplate(transactionManager).apply {
            this.isolationLevel = isolationLevel
            this.propagationBehavior = propagation
            this.timeout = timeout
            this.isReadOnly = readOnly
        }
        return transactionTemplate.execute { action() }!!
    }
}

