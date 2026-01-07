package site.rahoon.message.__monolitic.common.global.utils

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource
import org.springframework.transaction.annotation.TransactionManagementConfigurer
import org.springframework.transaction.interceptor.DefaultTransactionAttribute
import org.springframework.transaction.interceptor.TransactionAttribute
import org.springframework.transaction.interceptor.TransactionAttributeSource
import org.springframework.transaction.interceptor.TransactionInterceptor
import java.lang.reflect.Method

/**
 * 트랜잭션 기본 설정을 위한 Configuration
 * @Transactional의 기본 isolation level을 READ_COMMITTED로 설정
 */
@Configuration
class TransactionConfig(
    private val transactionManager: PlatformTransactionManager
) : TransactionManagementConfigurer {
    
    override fun annotationDrivenTransactionManager(): PlatformTransactionManager {
        return transactionManager
    }

    @Bean
    fun transactionInterceptor(): TransactionInterceptor {
        val interceptor = TransactionInterceptor()
        interceptor.transactionManager = transactionManager
        interceptor.transactionAttributeSource = customTransactionAttributeSource()
        return interceptor
    }

    private fun customTransactionAttributeSource(): TransactionAttributeSource {
        val defaultSource = AnnotationTransactionAttributeSource()
        
        return object : TransactionAttributeSource {
            override fun getTransactionAttribute(method: Method, targetClass: Class<*>?): TransactionAttribute? {
                val attr = method.let { defaultSource.getTransactionAttribute(it, targetClass) }
                if (attr != null && attr is DefaultTransactionAttribute) {
                    // isolation level이 명시되지 않은 경우 기본값으로 READ_COMMITTED 설정
                    if (attr.isolationLevel == TransactionDefinition.ISOLATION_DEFAULT) {
                        attr.isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED
                    }
                }
                return attr
            }

            override fun isCandidateClass(clazz: Class<*>): Boolean {
                return defaultSource.isCandidateClass(clazz)
            }
        }
    }
}

