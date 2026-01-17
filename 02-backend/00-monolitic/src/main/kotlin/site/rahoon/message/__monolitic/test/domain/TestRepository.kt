package site.rahoon.message.__monolitic.test.domain

/**
 * TestEntity를 위한 Repository 인터페이스
 * Soft Delete 기능 테스트를 위한 인터페이스입니다.
 */
interface TestRepository {
    fun findById(id: String): TestDomain?
    fun findAll(): List<TestDomain>
    fun save(entity: TestDomain): TestDomain
    fun deleteById(id: String)

    fun findByName(name: String): TestDomain?
    fun findWithDescriptionLike(descriptionPattern: String): List<TestDomain>
    fun findWithSelfJoin(): List<TestDomain>
}
