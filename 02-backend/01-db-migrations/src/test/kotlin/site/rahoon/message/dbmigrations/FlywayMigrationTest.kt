package site.rahoon.message.dbmigrations

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class FlywayMigrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true")
    }

    @Autowired
    lateinit var flyway: Flyway

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `Flyway 마이그레이션이 정상적으로 적용된다`() {
        // Spring Boot가 자동으로 마이그레이션을 실행함
        val info = flyway.info()

        println("=== Flyway Migration Info ===")
        println("Current version: ${info.current()?.version ?: "none"}")
        info.applied().forEach { migration ->
            println("  - ${migration.version}: ${migration.description} (${migration.state})")
        }

        // flyway_schema_history 테이블 존재 확인
        val tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'test_db'"
        )
        println("=== Tables in DB ===")
        tables.forEach { println("  - ${it["table_name"]}") }

        assertTrue(tables.isNotEmpty(), "테이블이 생성되어야 합니다")
    }
}
