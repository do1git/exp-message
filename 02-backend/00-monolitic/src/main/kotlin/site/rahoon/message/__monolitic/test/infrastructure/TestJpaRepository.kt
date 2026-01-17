package site.rahoon.message.__monolitic.test.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.infrastructure.JpaSoftDeleteRepository
import java.time.LocalDateTime

/**
 * TestEntity를 위한 Spring Data JPA Repository
 * Soft Delete된 엔티티는 Hibernate Filter로 자동으로 제외됩니다.
 * 
 * Note: Spring Data JPA의 기본 메서드(findById)와 메서드명 기반 쿼리는 
 * Hibernate Filter를 사용하지 않을 수 있으므로, @Query를 사용하여 명시적으로 조건을 추가합니다.
 */
interface TestJpaRepository : JpaSoftDeleteRepository<TestEntity, String> {
    fun findByName(name: String): TestEntity?
    
    @Query("""SELECT t FROM TestEntity t WHERE t.description LIKE %:pattern% """)
    fun findWithDescriptionLike(@Param("pattern") pattern: String): List<TestEntity>

    /**
     * Self-join 쿼리: 같은 이름을 가진 엔티티들을 조인하여 찾습니다.
     * Filter가 조인된 테이블에도 적용되는지 확인하기 위한 테스트용 쿼리입니다.
     */
    @Query("""
        SELECT t1 FROM TestEntity t1 
        INNER JOIN TestEntity t2 ON t1.name = t2.name 
        WHERE t1.id != t2.id
    """)
    fun findWithSelfJoin(): List<TestEntity>
}
