package site.rahoon.message.__monolitic.test.domain

import jakarta.persistence.*
import site.rahoon.message.__monolitic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime
import java.util.UUID

class TestDomain(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String? = null,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var deletedAt: LocalDateTime? = null,
)
