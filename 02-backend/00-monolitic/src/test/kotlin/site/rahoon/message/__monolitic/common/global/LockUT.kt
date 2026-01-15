package site.rahoon.message.__monolitic.common.global

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant

/**
 * Lock 단위 테스트
 * LockRepository를 Mock으로 처리하여 Lock 클래스의 로직만 검증합니다.
 */
class LockUT {

    private lateinit var lockRepository: LockRepository
    private lateinit var lock: Lock
    private var originalLock: Lock? = null

    @BeforeEach
    fun setUp() {
        // 원래 Lock.inst 백업 (초기화 안 됐을 수 있음)
        originalLock = runCatching { Lock.inst }.getOrNull()

        lockRepository = mockk()
        lock = Lock(lockRepository)
        lock.init() // companion object에 inst 설정
    }

    @AfterEach
    fun tearDown() {
        // 원래 Lock.inst 복구
        originalLock?.init()
    }

    @Test
    fun `단일 키 락 획득 성공 - LockToken 반환`() {
        // given
        val key = "test:key"
        val ttl = Duration.ofSeconds(5)
        val expectedToken = LockToken(key, "uuid-1234", Instant.now().plusSeconds(5))
        every { lockRepository.acquireLock(key, ttl) } returns expectedToken

        // when
        val token = lock.acquireLock(key, ttl)

        // then
        assertNotNull(token)
        assertEquals(expectedToken.lockId, token!!.lockId)
        verify { lockRepository.acquireLock(key, ttl) }
    }

    @Test
    fun `단일 키 락 획득 실패 - null 반환`() {
        // given
        val key = "test:key"
        val ttl = Duration.ofSeconds(5)
        every { lockRepository.acquireLock(key, ttl) } returns null

        // when
        val token = lock.acquireLock(key, ttl)

        // then
        assertNull(token)
        verify { lockRepository.acquireLock(key, ttl) }
    }

    @Test
    fun `락 해제 성공 - 올바른 토큰`() {
        // given
        val token = LockToken("test:key", "uuid-1234", Instant.now().plusSeconds(5))
        every { lockRepository.releaseLock(token) } returns true

        // when
        val result = lock.releaseLock(token)

        // then
        assertTrue(result)
        verify { lockRepository.releaseLock(token) }
    }

    @Test
    fun `락 해제 실패 - 잘못된 토큰`() {
        // given
        val wrongToken = LockToken("test:key", "wrong-uuid", Instant.now().plusSeconds(5))
        every { lockRepository.releaseLock(wrongToken) } returns false

        // when
        val result = lock.releaseLock(wrongToken)

        // then
        assertFalse(result)
        verify { lockRepository.releaseLock(wrongToken) }
    }

    @Test
    fun `다중 키 락 획득 성공 - LockToken 반환`() {
        // given
        val keys = listOf("key1", "key2")
        val ttl = Duration.ofSeconds(5)
        val expectedToken = LockToken(keys, "uuid-multi", Instant.now().plusSeconds(5))
        every { lockRepository.acquireLocks(keys, ttl) } returns expectedToken

        // when
        val token = lock.acquireLocks(keys, ttl)

        // then
        assertNotNull(token)
        assertEquals(expectedToken.lockId, token!!.lockId)
        assertEquals(keys, token.keys)
        verify { lockRepository.acquireLocks(keys, ttl) }
    }

    @Test
    fun `다중 키 락 획득 실패 - null 반환`() {
        // given
        val keys = listOf("key1", "key2")
        val ttl = Duration.ofSeconds(5)
        every { lockRepository.acquireLocks(keys, ttl) } returns null

        // when
        val token = lock.acquireLocks(keys, ttl)

        // then
        assertNull(token)
        verify { lockRepository.acquireLocks(keys, ttl) }
    }

    @Test
    fun `execute - 단일 키 락 획득 후 action 실행 및 토큰으로 락 해제`() {
        // given
        val key = "test:execute"
        val keys = listOf(key)
        val token = LockToken(keys, "uuid-execute", Instant.now().plusSeconds(5))
        every { lockRepository.acquireLocks(keys, any()) } returns token
        every { lockRepository.releaseLock(token) } returns true

        // when
        val result = Lock.execute(key) { locked, lockToken ->
            assertTrue(locked)
            assertNotNull(lockToken)
            "action result"
        }

        // then
        assertEquals("action result", result)
        verify { lockRepository.acquireLocks(keys, any()) }
        verify { lockRepository.releaseLock(token) }
    }

    @Test
    fun `execute - 다중 키 락 획득 후 action 실행 및 락 해제`() {
        // given
        val keys = listOf("key1", "key2")
        val token = LockToken(keys, "uuid-multi-execute", Instant.now().plusSeconds(5))
        every { lockRepository.acquireLocks(keys, any()) } returns token
        every { lockRepository.releaseLock(token) } returns true

        // when
        val result = Lock.execute(keys) { locked, lockToken ->
            assertTrue(locked)
            assertNotNull(lockToken)
            42
        }

        // then
        assertEquals(42, result)
        verify { lockRepository.acquireLocks(keys, any()) }
        verify { lockRepository.releaseLock(token) }
    }

    @Test
    fun `execute - 락 획득 실패 시 action에 locked=false 전달`() {
        // given
        val keys = listOf("key1", "key2")
        every { lockRepository.acquireLocks(keys, any()) } returns null

        // when
        val result = Lock.execute(keys) { locked, lockToken ->
            assertFalse(locked)
            assertNull(lockToken)
            "fallback result"
        }

        // then
        assertEquals("fallback result", result)
        verify { lockRepository.acquireLocks(keys, any()) }
        verify(exactly = 0) { lockRepository.releaseLock(any()) }
    }

    @Test
    fun `execute - 락 획득 실패 시 lockFailException 전달하면 예외 발생`() {
        // given
        val keys = listOf("key1", "key2")
        every { lockRepository.acquireLocks(keys, any()) } returns null
        val exception = RuntimeException("Lock failed")

        // when & then
        val thrown = assertThrows<RuntimeException> {
            Lock.execute(keys, lockFailException = exception) { locked, _ ->
                "should not execute"
            }
        }

        assertEquals("Lock failed", thrown.message)
        verify { lockRepository.acquireLocks(keys, any()) }
        verify(exactly = 0) { lockRepository.releaseLock(any()) }
    }

    @Test
    fun `execute - 단일 키, action에서 예외 발생해도 락 해제됨`() {
        // given
        val key = "test:exception"
        val keys = listOf(key)
        val token = LockToken(keys, "uuid-exception", Instant.now().plusSeconds(5))
        every { lockRepository.acquireLocks(keys, any()) } returns token
        every { lockRepository.releaseLock(token) } returns true

        // when & then
        assertThrows<RuntimeException> {
            Lock.execute(key) { locked, _ ->
                assertTrue(locked)
                throw RuntimeException("action failed")
            }
        }

        // finally에서 락 해제가 호출되었는지 확인
        verify { lockRepository.acquireLocks(keys, any()) }
        verify { lockRepository.releaseLock(token) }
    }

    @Test
    fun `LockToken isExpired - 만료되지 않은 토큰`() {
        // given
        val token = LockToken("key", "uuid", Instant.now().plusSeconds(10))

        // then
        assertFalse(token.isExpired())
    }

    @Test
    fun `LockToken isExpired - 만료된 토큰`() {
        // given
        val token = LockToken("key", "uuid", Instant.now().minusSeconds(1))

        // then
        assertTrue(token.isExpired())
    }
}
