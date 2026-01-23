package site.rahoon.message.monolithic.common.test

import com.redis.testcontainers.RedisContainer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Paths
import java.time.Duration

/**
 * MySQL + Redis Testcontainers 기반 통합 테스트 베이스 클래스
 *
 * - @IntegrationTest 포함 (SpringBootTest + 태그)
 * - Singleton Container Pattern: 모든 테스트에서 컨테이너 재사용
 * - 컨테이너 시작 후 자동으로 DB 마이그레이션 실행
 *
 * 사용법:
 * ```kotlin
 * class MyControllerIT : IntegrationTestBase() {
 *     @Test
 *     fun myTest() { ... }
 * }
 * ```
 */
@IntegrationTest
abstract class IntegrationTestBase {
    protected val logger = KotlinLogging.logger { }

    /**
     * 테스트마다 고유한 이메일을 생성합니다.
     */
    protected fun uniqueEmail(prefix: String = "test"): String = TestUtils.uniqueEmail(prefix)

    /**
     * 테스트용 고유 IP 주소를 생성합니다.
     */
    protected fun uniqueIp(): String = TestUtils.uniqueIp()

    companion object {
        private val network: Network = Network.newNetwork()

        // Singleton Container Pattern - 모든 테스트에서 재사용
        val mysql: MySQLContainer<*> =
            MySQLContainer("mysql:8.0")
                .withNetwork(network)
                .withNetworkAliases("mysql")
                .withDatabaseName("test_db")
                .withUsername("test_user")
                .withPassword("test_password")
                .withUrlParam("useSSL", "false")
                .withUrlParam("allowPublicKeyRetrieval", "true")

        val redis: RedisContainer =
            RedisContainer("redis:7-alpine")
                .withNetwork(network)
                .withNetworkAliases("redis")

        init {
            // 컨테이너 시작 (한 번만 실행됨)
            mysql.start()
            redis.start()

            // 마이그레이션 실행
            runMigrations()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // MySQL
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.MySQLDialect" }
            registry.add("decorator.datasource.p6spy.enable-logging") { true }

            // Redis
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }

        /**
         * 01-db-migrations Docker 이미지를 빌드하고 마이그레이션을 실행합니다.
         */
        private fun runMigrations() {
            println("=== Running DB migrations ===")
            val migrationImage =
                ImageFromDockerfile()
                    .withDockerfile(Paths.get("../01-db-migrations/Dockerfile"))

            GenericContainer(migrationImage)
                .withNetwork(network)
                .withEnv("DB_HOST", "mysql")
                .withEnv("DB_PORT", "3306")
                .withEnv("DB_NAME", "test_db")
                .withEnv("DB_USERNAME", "test_user")
                .withEnv("DB_PASSWORD", "test_password")
                .waitingFor(Wait.forLogMessage(".*Started DbMigrationsApplication.*", 1))
                .withStartupTimeout(Duration.ofMinutes(3))
                .start()

            println("=== Migration completed ===")
        }
    }
}
