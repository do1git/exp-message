package site.rahoon.message.__monolitic.user.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.user.domain.component.BCryptUserPasswordHasher
import site.rahoon.message.__monolitic.user.infrastructure.UserJpaRepository
import site.rahoon.message.__monolitic.user.infrastructure.UserRepositoryImpl

@DataJpaTest
@Import(UserRepositoryImpl::class, BCryptUserPasswordHasher::class, UserDomainService::class)
class UserDomainServiceTest {

    @Autowired
    private lateinit var jpaRepository: UserJpaRepository

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @Autowired
    private lateinit var passwordHasher: BCryptUserPasswordHasher

    private lateinit var userDomainService: UserDomainService

    @BeforeEach
    fun setUp() {
        userDomainService = UserDomainService(userRepository, passwordHasher)
    }

    @Test
    fun `사용자 생성 성공`() {
        // given
        val command = UserCommand.Create(
            email = "test@example.com",
            password = "password123",
            nickname = "testuser"
        )

        // when
        val user = userDomainService.create(command)

        // then
        assertNotNull(user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("testuser", user.nickname)
        assertNotEquals("password123", user.passwordHash) // 해시된 비밀번호
        assertNotNull(user.createdAt)
        assertNotNull(user.updatedAt)
    }

    @Test
    fun `이메일 중복 시 예외 발생`() {
        // given
        val command1 = UserCommand.Create(
            email = "duplicate@example.com",
            password = "password123",
            nickname = "user1"
        )
        val command2 = UserCommand.Create(
            email = "duplicate@example.com",
            password = "password456",
            nickname = "user2"
        )

        // when
        userDomainService.create(command1)

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.create(command2)
        }
        assertEquals(UserError.EMAIL_ALREADY_EXISTS, exception.error)
    }

    @Test
    fun `사용자 닉네임 업데이트 성공`() {
        // given
        val createCommand = UserCommand.Create(
            email = "update@example.com",
            password = "password123",
            nickname = "oldnickname"
        )
        val user = userDomainService.create(createCommand)
        val updateCommand = UserCommand.Update(
            id = user.id,
            nickname = "newnickname"
        )

        // when
        val updatedUser = userDomainService.update(updateCommand)

        // then
        assertEquals("newnickname", updatedUser.nickname)
        assertEquals(user.id, updatedUser.id)
        assertEquals(user.email, updatedUser.email)
        assertTrue(updatedUser.updatedAt.isAfter(user.updatedAt))
    }

    @Test
    fun `존재하지 않는 사용자 업데이트 시 예외 발생`() {
        // given
        val updateCommand = UserCommand.Update(
            id = "non-existent-id",
            nickname = "newnickname"
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.update(updateCommand)
        }
        assertEquals(UserError.USER_NOT_FOUND, exception.error)
    }

    @Test
    fun `사용자 삭제 성공`() {
        // given
        val createCommand = UserCommand.Create(
            email = "delete@example.com",
            password = "password123",
            nickname = "todelete"
        )
        val user = userDomainService.create(createCommand)
        val deleteCommand = UserCommand.Delete(id = user.id)

        // when
        val deletedUser = userDomainService.delete(deleteCommand)

        // then
        assertEquals(user.id, deletedUser.id)
        assertNull(userRepository.findById(user.id))
    }

    @Test
    fun `존재하지 않는 사용자 삭제 시 예외 발생`() {
        // given
        val deleteCommand = UserCommand.Delete(id = "non-existent-id")

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.delete(deleteCommand)
        }
        assertEquals(UserError.USER_NOT_FOUND, exception.error)
    }
}

