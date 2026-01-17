package site.rahoon.message.__monolitic.common.infrastructure.config

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import site.rahoon.message.__monolitic.common.infrastructure.JpaSoftDeleteRepositoryImpl

/**
 * JPA 설정
 * Soft Delete를 위한 Hibernate Filter를 자동으로 활성화합니다.
 * 
 * FilterDefinition과 Filter 적용은 SoftDeleteFilterMetadataContributor에서 처리합니다.
 * 이 설정은 각 트랜잭션마다 필터를 활성화하는 역할만 합니다.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = ["site.rahoon.message.__monolitic"],
    repositoryBaseClass = JpaSoftDeleteRepositoryImpl::class
)
class JpaConfig {

    /**
     * JpaTransactionManager를 커스터마이징하여
     * 각 트랜잭션마다 softDeleteFilter를 자동으로 활성화합니다.
     */
    @Bean
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        return object : JpaTransactionManager(entityManagerFactory) {
            override fun createEntityManagerForTransaction(): EntityManager {
                val entityManager = super.createEntityManagerForTransaction()
                enableSoftDeleteFilter(entityManager)
                return entityManager
            }

            override fun doBegin(transaction: Any, definition: org.springframework.transaction.TransactionDefinition) {
                super.doBegin(transaction, definition)
                // 트랜잭션 시작 시 필터 활성화
                // TransactionSynchronizationManager를 통해 현재 트랜잭션에 바인딩된 EntityManager 가져오기
                val entityManagerHolder = org.springframework.transaction.support.TransactionSynchronizationManager
                    .getResource(entityManagerFactory) as? org.springframework.orm.jpa.EntityManagerHolder
                entityManagerHolder?.entityManager?.let { enableSoftDeleteFilter(it) }
            }

            private fun enableSoftDeleteFilter(entityManager: EntityManager) {
                try {
                    val session = entityManager.unwrap(org.hibernate.Session::class.java)
                    session.enableFilter("softDeleteFilter")
                } catch (e: Exception) {
                    // Hibernate Session을 unwrap할 수 없는 경우 무시
                    // (예: 다른 JPA 구현체를 사용하는 경우)
                }
            }
        }
    }
}
