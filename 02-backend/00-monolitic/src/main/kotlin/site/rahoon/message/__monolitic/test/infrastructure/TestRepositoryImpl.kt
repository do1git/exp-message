package site.rahoon.message.__monolitic.test.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.global.TransactionalRepository
import site.rahoon.message.__monolitic.test.domain.TestDomain
import site.rahoon.message.__monolitic.test.domain.TestRepository
import java.time.LocalDateTime

@TransactionalRepository
class TestRepositoryImpl(
    private val testJpaRepository: TestJpaRepository,
) : TestRepository {

    override fun findByName(name: String): TestDomain? {
        return testJpaRepository.findByName(name)?.let { toDomain(it) }
    }

    override fun findWithDescriptionLike(descriptionPattern: String): List<TestDomain> {
        return testJpaRepository.findWithDescriptionLike(descriptionPattern).map { toDomain(it) }
    }

    override fun findWithSelfJoin(): List<TestDomain> {
        return testJpaRepository.findWithSelfJoin().map { toDomain(it) }
    }

    override fun findById(id: String): TestDomain? {
        return testJpaRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findAll(): List<TestDomain> {
        return testJpaRepository.findAll().map { toDomain(it) }
    }

    @Transactional
    override fun save(entity: TestDomain): TestDomain {
        return toDomain(testJpaRepository.save(fromDomain(entity)))
    }

    @Transactional
    override fun deleteById(id:String){
        testJpaRepository.softDeleteById(id, LocalDateTime.now())
    }


    fun create(id: String, name: String, description: String? = null): TestDomain{
        return save(TestDomain(id,name, description))
    }

    private fun toDomain(entity: TestEntity): TestDomain {
        return TestDomain(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdAt = entity.createdAt,
            deletedAt = entity.deletedAt,
        )
    }

    private fun fromDomain(domain: TestDomain): TestEntity {
        return TestEntity(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            createdAt = domain.createdAt,
        )
    }
}
