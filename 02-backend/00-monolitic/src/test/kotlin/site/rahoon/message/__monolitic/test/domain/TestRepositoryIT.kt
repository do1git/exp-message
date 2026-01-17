package site.rahoon.message.__monolitic.test.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import site.rahoon.message.__monolitic.common.domain.SoftDeleteContext
import site.rahoon.message.__monolitic.common.global.Tx
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import site.rahoon.message.__monolitic.test.infrastructure.TestRepositoryImpl
import java.util.UUID

/**
 * TestRepository 통합 테스트
 * 실제 MySQL(Testcontainers)을 사용하여 Soft Delete 기능을 검증합니다.
 */
class TestRepositoryIT : IntegrationTestBase() {

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var testRepositoryImpl: TestRepositoryImpl

    @BeforeEach
    fun setUp() {
        // 테스트 전 데이터 정리 (필요한 경우)
    }

    @Test
    fun `엔티티 저장 성공`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
                id = id,
                name = "test-entity",
                description = "테스트 엔티티"
        )

        // when
        val saved = testRepository.save(entity)

        // then
        assertNotNull(saved)
        assertEquals(id, saved.id)
        assertEquals("test-entity", saved.name)
        assertEquals("테스트 엔티티", saved.description)
        assertNotNull(saved.createdAt)
    }

    @Test
    fun `ID로 엔티티 조회 성공`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "find-by-id-test"
        )
        testRepository.save(entity)

        // when
        val found = testRepository.findById(id)

        // then
        assertNotNull(found)
        assertEquals(id, found?.id)
        assertEquals("find-by-id-test", found?.name)
    }

    @Test
    fun `이름으로 엔티티 조회 성공`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "find-by-name-test"
        )
        testRepository.save(entity)

        // when
        val found = testRepository.findByName("find-by-name-test")

        // then
        assertNotNull(found)
        assertEquals(id, found?.id)
        assertEquals("find-by-name-test", found?.name)
    }

    @Test
    fun `전체 엔티티 조회 성공`() {
        // given
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "find-all-test-1"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "find-all-test-2"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)

        // when
        val all = testRepository.findAll()

        // then
        assertTrue(all.size >= 2)
        assertTrue(all.any { it.name == "find-all-test-1" })
        assertTrue(all.any { it.name == "find-all-test-2" })
    }

    @Test
    fun `Soft Delete 후 조회되지 않음 - findById`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "soft-delete-test"
        )
        testRepository.save(entity)

        // when
        testRepository.deleteById(id)

        // then
        val found = testRepository.findById(id)
        assertNull(found, "Soft Delete된 엔티티는 조회되지 않아야 합니다")
    }

    @Test
    fun `Soft Delete 후 조회되지 않음 - findByName`() {
        // given
        val id = UUID.randomUUID().toString()
        val name = "soft-delete-name-test"
        val entity = testRepositoryImpl.create(
            id = id,
            name = name
        )
        testRepository.save(entity)

        // when
        testRepository.deleteById(id)

        // then
        val found = testRepository.findByName(name)
        assertNull(found, "Soft Delete된 엔티티는 이름으로 조회되지 않아야 합니다")
    }

    @Test
    fun `Soft Delete 후 전체 조회에서 제외됨`() {
        // given
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val entity1 = testRepositoryImpl.create(
            id = id1,
            name = "soft-delete-all-test-1"
        )
        val entity2 = testRepositoryImpl.create(
            id = id2,
            name = "soft-delete-all-test-2"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)

        // when
        testRepository.deleteById(id1)
        val all = testRepository.findAll()

        // then
        assertFalse(all.any { it.id == id1 }, "Soft Delete된 엔티티는 전체 조회에서 제외되어야 합니다")
        assertTrue(all.any { it.id == id2 }, "삭제되지 않은 엔티티는 조회되어야 합니다")
    }

    @Test
    fun `존재하지 않는 엔티티 조회 시 null 반환`() {
        // when
        val found = testRepository.findById("non-existent-id")

        // then
        assertNull(found)
    }

    @Test
    fun `존재하지 않는 이름으로 조회 시 null 반환`() {
        // when
        val found = testRepository.findByName("non-existent-name")

        // then
        assertNull(found)
    }

    @Test
    fun `여러 엔티티 저장 후 전체 조회`() {
        // given
        val entities = (1..5).map { i ->
            testRepositoryImpl.create(
                id = UUID.randomUUID().toString(),
                name = "batch-test-$i",
                description = "배치 테스트 $i"
            )
        }

        // when
        entities.forEach { testRepository.save(it) }
        val all = testRepository.findAll()

        // then
        assertTrue(all.size >= 5)
        entities.forEach { entity ->
            assertTrue(all.any { it.id == entity.id }, "저장한 엔티티 ${entity.id}가 조회되어야 합니다")
        }
    }

    @Test
    fun `Soft Delete 후 다시 저장 가능`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity1 = testRepositoryImpl.create(
            id = id,
            name = "recreate-test"
        )
        testRepository.save(entity1)
        testRepository.deleteById(id)

        // when - 같은 ID로 다시 저장
        val entity2 = testRepositoryImpl.create(
            id = id,
            name = "recreate-test-2"
        )
        val saved = testRepository.save(entity2)

        // then
        assertNotNull(saved)
        assertEquals(id, saved.id)
        assertEquals("recreate-test-2", saved.name)
    }

    @Test
    fun `조인 쿼리로 설명 패턴 검색 성공`() {
        // given
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-test-1",
            description = "테스트 설명입니다"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-test-2",
            description = "다른 설명입니다"
        )
        val entity3 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-test-3",
            description = null
        )
        testRepository.save(entity1)
        testRepository.save(entity2)
        testRepository.save(entity3)

        // when
        val found = testRepository.findWithDescriptionLike("테스트")

        // then
        assertTrue(found.size >= 1)
        assertTrue(found.any { it.id == entity1.id }, "패턴이 일치하는 엔티티가 조회되어야 합니다")
        assertFalse(found.any { it.id == entity2.id }, "패턴이 일치하지 않는 엔티티는 조회되지 않아야 합니다")
        assertFalse(found.any { it.id == entity3.id }, "description이 null인 엔티티는 조회되지 않아야 합니다")
    }

    @Test
    fun `조인 쿼리로 설명 패턴 검색 - Soft Delete된 엔티티 제외`() {
        // given
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-soft-delete-test-1",
            description = "삭제될 설명"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-soft-delete-test-2",
            description = "유지될 설명"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)

        // when
        testRepository.deleteById(entity1.id)
        val found = testRepository.findWithDescriptionLike("설명")

        // then
        assertTrue(found.any { it.id == entity2.id }, "삭제되지 않은 엔티티는 조회되어야 합니다")
        assertFalse(found.any { it.id == entity1.id }, "Soft Delete된 엔티티는 조회되지 않아야 합니다")
    }

    @Test
    fun `Soft Delete Filter 비활성화 후 Soft Delete된 엔티티도 조회 가능`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "filter-disable-test"
        )
        testRepository.save(entity)
        testRepository.deleteById(id)

        // when - 필터 비활성화 후 조회
        val found = Tx.execute {
            SoftDeleteContext.disable {
                testRepository.findById(id)
            }
        }

        // then
        assertNotNull(found, "필터가 비활성화된 경우 Soft Delete된 엔티티도 조회되어야 합니다")
        assertEquals(id, found?.id)
        assertNotNull(found?.deletedAt, "Soft Delete된 엔티티는 deletedAt이 설정되어 있어야 합니다")
    }

    @Test
    fun `Soft Delete Filter 비활성화 후 전체 조회에서 Soft Delete된 엔티티도 포함`() {
        // given
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val entity1 = testRepositoryImpl.create(
            id = id1,
            name = "filter-disable-all-test-1"
        )
        val entity2 = testRepositoryImpl.create(
            id = id2,
            name = "filter-disable-all-test-2"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)
        testRepository.deleteById(id1)

        // when - 필터 비활성화 후 전체 조회
        // JpaSoftDeleteSession.disable을 사용하여 필터를 비활성화하고 조회
        val all = Tx.execute {
            SoftDeleteContext.disable {
                testRepository.findAll()
            }
        }

        // then
        all!!
        assertTrue(all.any { it.id == id1 }, "필터가 비활성화된 경우 Soft Delete된 엔티티도 조회되어야 합니다")
        assertTrue(all.any { it.id == id2 }, "삭제되지 않은 엔티티는 조회되어야 합니다")
        
        val deletedEntity = all.find { it.id == id1 }
        assertNotNull(deletedEntity?.deletedAt, "Soft Delete된 엔티티는 deletedAt이 설정되어 있어야 합니다")
    }

    @Test
    fun `Soft Delete Filter 비활성화 후 이름으로 조회 시 Soft Delete된 엔티티도 조회 가능`() {
        // given
        val id = UUID.randomUUID().toString()
        val name = "filter-disable-name-test"
        val entity = testRepositoryImpl.create(
            id = id,
            name = name
        )
        testRepository.save(entity)
        testRepository.deleteById(id)

        // when - 필터 비활성화 후 이름으로 조회
        // JpaSoftDeleteSession.disable을 사용하여 필터를 비활성화하고 조회
        val found =
            Tx.execute {
            SoftDeleteContext.disable {
                testRepository.findByName(name)
            }
        }

        // then
        assertNotNull(found, "필터가 비활성화된 경우 Soft Delete된 엔티티도 조회되어야 합니다")
        assertEquals(id, found?.id)
        assertNotNull(found?.deletedAt, "Soft Delete된 엔티티는 deletedAt이 설정되어 있어야 합니다")
    }

    @Test
    fun `Self-join 쿼리에서 Filter 적용 확인 - 삭제되지 않은 엔티티만 조회`() {
        // given: 같은 이름을 가진 엔티티들을 생성
        val name = "self-join-test"
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "엔티티 1"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "엔티티 2"
        )
        val entity3 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "엔티티 3"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)
        testRepository.save(entity3)

        // when: Self-join 쿼리 실행
        val found = testRepository.findWithSelfJoin()

        // then: 같은 이름을 가진 엔티티들이 조회되어야 함 (자기 자신 제외)
        assertTrue(found.size >= 2, "같은 이름을 가진 엔티티들이 조회되어야 합니다")
        assertTrue(found.any { it.id == entity1.id || it.id == entity2.id || it.id == entity3.id })
    }

    @Test
    fun `Self-join 쿼리에서 Filter 적용 확인 - Soft Delete된 엔티티는 조인에서 제외`() {
        // given: 같은 이름을 가진 엔티티들을 생성
        val name = "self-join-soft-delete-test"
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "삭제될 엔티티"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "유지될 엔티티 1"
        )
        val entity3 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "유지될 엔티티 2"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)
        testRepository.save(entity3)

        // when: entity1을 Soft Delete하고 Self-join 쿼리 실행
        testRepository.deleteById(entity1.id)
        val found = testRepository.findWithSelfJoin()

        // then: 
        // 1. Soft Delete된 entity1은 조회되지 않아야 함
        assertFalse(found.any { it.id == entity1.id }, "Soft Delete된 엔티티는 조회되지 않아야 합니다")
        
        // 2. entity2와 entity3는 조회되어야 함 (서로 같은 이름이므로)
        // entity2와 entity3가 서로 조인되어 조회됨
        val foundIds = found.map { it.id }.toSet()
        assertTrue(foundIds.contains(entity2.id) || foundIds.contains(entity3.id), 
            "삭제되지 않은 엔티티는 조회되어야 합니다")
    }

    @Test
    fun `Self-join 쿼리에서 Filter 적용 확인 - 조인된 양쪽 테이블 모두 Filter 적용`() {
        // given: 같은 이름을 가진 엔티티들을 생성
        val name = "self-join-both-filter-test"
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "엔티티 1"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "엔티티 2"
        )
        val entity3 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "엔티티 3"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)
        testRepository.save(entity3)

        // when: entity1과 entity2를 Soft Delete하고 Self-join 쿼리 실행
        testRepository.deleteById(entity1.id)
        testRepository.deleteById(entity2.id)
        val found = testRepository.findWithSelfJoin()

        // then: 
        // 1. Soft Delete된 entity1, entity2는 조회되지 않아야 함
        assertFalse(found.any { it.id == entity1.id }, "Soft Delete된 엔티티1은 조회되지 않아야 합니다")
        assertFalse(found.any { it.id == entity2.id }, "Soft Delete된 엔티티2는 조회되지 않아야 합니다")
        
        // 2. entity3만 남았는데, 같은 이름을 가진 다른 엔티티가 없으므로 
        //    Self-join 조건(t1.id != t2.id)을 만족하는 조합이 없어야 함
        //    단, 다른 테스트에서 생성된 같은 이름의 엔티티가 있을 수 있으므로
        //    entity3가 조회되지 않는다고 단정할 수는 없음
        //    하지만 entity1, entity2는 확실히 조회되지 않아야 함
    }
}
