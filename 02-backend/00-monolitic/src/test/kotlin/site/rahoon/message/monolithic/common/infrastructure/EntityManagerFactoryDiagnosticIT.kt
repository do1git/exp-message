package site.rahoon.message.monolithic.common.infrastructure

import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import site.rahoon.message.monolithic.common.test.IntegrationTestBase

/**
 * EntityManagerFactory 빈 진단 테스트
 *
 * EMF가 여러 개 생성되는지 확인하기 위한 디버깅용 테스트.
 * 실행: `./gradlew integrationTest --tests "*EntityManagerFactoryDiagnosticIT"`
 */
class EntityManagerFactoryDiagnosticIT : IntegrationTestBase() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `EMF 빈 개수 및 identityHashCode 출력`() {
        val emfBeans = applicationContext.getBeansOfType<EntityManagerFactory>()
        val tmBeans = applicationContext.getBeansOfType<PlatformTransactionManager>()

        println("=== EntityManagerFactory 진단 ===")
        println("EMF 빈 개수: ${emfBeans.size}")
        emfBeans.forEach { (name, emf) ->
            println("  - $name: identityHashCode=${System.identityHashCode(emf)}")
        }

        println("")
        println("PlatformTransactionManager (전체 ${tmBeans.size}개):")
        tmBeans.forEach { (name, tm) ->
            println("  - $name: ${tm.javaClass.simpleName}")
        }
        println("")
        println("JpaTransactionManager → EMF:")
        tmBeans.filterValues { it is JpaTransactionManager }.forEach { (name, tm) ->
            val jpaTm = tm as JpaTransactionManager
            val emf = jpaTm.entityManagerFactory
            println("  - $name: emf.identityHashCode=${System.identityHashCode(emf)}")
        }

        if (emfBeans.size > 1) {
            println("")
            println("⚠️ EMF가 ${emfBeans.size}개 발견됨 - 원인 조사 필요")
        }
    }
}
