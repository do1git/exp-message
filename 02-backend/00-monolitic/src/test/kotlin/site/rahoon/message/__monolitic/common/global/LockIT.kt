package site.rahoon.message.__monolitic.common.global

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Lock 통합 테스트
 * 실제 Redis를 사용하여 분산 락 동작을 검증합니다.
 */
class LockIT : IntegrationTestBase() {

    @Autowired
    private lateinit var lock: Lock

    @Test
    fun `단일 키 락 획득 및 해제`() {
        // given
        val key = "it:test:${System.nanoTime()}"

        // when
        val token = lock.acquireLock(key, Duration.ofSeconds(5))

        // then
        assertNotNull(token)
        assertEquals(listOf(key), token!!.keys)
        assertFalse(token.isExpired())

        // cleanup
        val released = lock.releaseLock(token)
        assertTrue(released)
    }

    @Test
    fun `이미 획득된 락은 다시 획득 불가`() {
        // given
        val key = "it:test:duplicate:${System.nanoTime()}"
        val token = lock.acquireLock(key, Duration.ofSeconds(5))

        // when
        val secondToken = lock.acquireLock(key, Duration.ofSeconds(5))

        // then
        assertNull(secondToken)

        // cleanup
        lock.releaseLock(token!!)
    }

    @Test
    fun `락 해제 후 다시 획득 가능`() {
        // given
        val key = "it:test:reacquire:${System.nanoTime()}"
        val token = lock.acquireLock(key, Duration.ofSeconds(5))
        lock.releaseLock(token!!)

        // when
        val newToken = lock.acquireLock(key, Duration.ofSeconds(5))

        // then
        assertNotNull(newToken)

        // cleanup
        lock.releaseLock(newToken!!)
    }

    @Test
    fun `잘못된 토큰으로는 락 해제 불가`() {
        // given
        val key = "it:test:wrongtoken:${System.nanoTime()}"
        val token = lock.acquireLock(key, Duration.ofSeconds(5))

        // when - 잘못된 토큰으로 해제 시도
        val wrongToken = LockToken(key, "wrong-lock-id", Instant.now().plusSeconds(5))
        val released = lock.releaseLock(wrongToken)

        // then
        assertFalse(released)

        // 원래 토큰으로는 해제 가능
        val correctRelease = lock.releaseLock(token!!)
        assertTrue(correctRelease)
    }

    @Test
    fun `다중 키 락 all-or-nothing 획득`() {
        // given
        val keys = listOf(
            "it:multi:1:${System.nanoTime()}",
            "it:multi:2:${System.nanoTime()}"
        )

        // when
        val token = lock.acquireLocks(keys, Duration.ofSeconds(5))

        // then
        assertNotNull(token)
        assertEquals(keys.sorted(), token!!.keys)

        // cleanup
        lock.releaseLock(token)
    }

    @Test
    fun `다중 키 락 - 하나라도 이미 획득되어 있으면 전체 실패`() {
        // given
        val key1 = "it:partial:1:${System.nanoTime()}"
        val key2 = "it:partial:2:${System.nanoTime()}"

        // key1만 먼저 획득
        val token1 = lock.acquireLock(key1, Duration.ofSeconds(5))

        // when - key1, key2 동시 획득 시도
        val multiToken = lock.acquireLocks(listOf(key1, key2), Duration.ofSeconds(5))

        // then
        assertNull(multiToken)

        // key2는 획득되지 않았어야 함
        val token2 = lock.acquireLock(key2, Duration.ofSeconds(5))
        assertNotNull(token2)

        // cleanup
        lock.releaseLock(token1!!)
        lock.releaseLock(token2!!)
    }

    @Test
    fun `execute - 락 범위 내에서만 action 실행`() {
        // given
        val key = "it:execute:${System.nanoTime()}"

        // when
        val result = Lock.execute(key) { locked, lockToken ->
            assertTrue(locked)
            assertNotNull(lockToken)
            "executed"
        }

        // then
        assertEquals("executed", result)

        // 락이 해제되었으므로 다시 획득 가능
        val token = lock.acquireLock(key, Duration.ofSeconds(5))
        assertNotNull(token)
        lock.releaseLock(token!!)
    }

    @Test
    fun `execute - 락 획득 실패 시 action에 locked=false 전달`() {
        // given
        val key = "it:execute:fail:${System.nanoTime()}"
        val token = lock.acquireLock(key, Duration.ofSeconds(5))

        // when
        val result = Lock.execute(key) { locked, lockToken ->
            assertFalse(locked)
            assertNull(lockToken)
            "fallback"
        }

        // then
        assertEquals("fallback", result)

        // cleanup
        lock.releaseLock(token!!)
    }

    @Test
    fun `동시성 테스트 - 여러 스레드에서 같은 키에 락 획득 시도`() {
        // given
        val key = "it:concurrent:${System.nanoTime()}"
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)
        val acquiredToken = AtomicReference<LockToken?>(null)
        val latch = CountDownLatch(threadCount)

        // when
        repeat(threadCount) {
            executor.submit {
                try {
                    val token = lock.acquireLock(key, Duration.ofSeconds(5))
                    if (token != null) {
                        successCount.incrementAndGet()
                        acquiredToken.set(token)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        // then - 단 하나의 스레드만 락 획득 성공
        assertEquals(1, successCount.get())

        // cleanup
        acquiredToken.get()?.let { lock.releaseLock(it) }
        executor.shutdown()
    }

    @Test
    fun `동시성 테스트 - execute로 순차 실행 보장`() {
        // given
        val key = "it:concurrent:execute:${System.nanoTime()}"
        val threadCount = 5
        val executor = Executors.newFixedThreadPool(threadCount)
        val counter = AtomicInteger(0)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)

        // when
        repeat(threadCount) {
            executor.submit {
                try {
                    Lock.execute(key) { locked, _ ->
                        if (locked) {
                            successCount.incrementAndGet()
                            Thread.sleep(50) // 작업 시뮬레이션
                            counter.incrementAndGet()
                        } else {
                            failCount.incrementAndGet()
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        // then - 락 획득 성공 + 실패 = 전체 스레드 수
        assertEquals(threadCount, successCount.get() + failCount.get())
        // 성공한 만큼만 counter 증가
        assertEquals(successCount.get(), counter.get())

        executor.shutdown()
    }

    @Test
    fun `다른 클라이언트의 락은 해제할 수 없음`() {
        // given
        val key = "it:ownership:${System.nanoTime()}"
        val token1 = lock.acquireLock(key, Duration.ofSeconds(5))
        assertNotNull(token1)

        // when - 다른 토큰으로 해제 시도
        val fakeToken = LockToken(key, "fake-lock-id", Instant.now().plusSeconds(5))
        val releaseWithWrongToken = lock.releaseLock(fakeToken)

        // then
        assertFalse(releaseWithWrongToken)

        // 락이 여전히 유효하므로 새로운 획득 시도 실패
        val token2 = lock.acquireLock(key, Duration.ofSeconds(5))
        assertNull(token2)

        // cleanup - 올바른 토큰으로 해제
        val releaseWithCorrectToken = lock.releaseLock(token1!!)
        assertTrue(releaseWithCorrectToken)
    }

    @Test
    fun `LockToken에 expiresAt 정보가 포함되어 있음`() {
        // given
        val key = "it:expiry:${System.nanoTime()}"
        val ttl = Duration.ofSeconds(5)

        // when
        val token = lock.acquireLock(key, ttl)

        // then
        assertNotNull(token)
        assertTrue(token!!.expiresAt.isAfter(Instant.now()))
        assertTrue(token.expiresAt.isBefore(Instant.now().plusSeconds(6)))

        // cleanup
        lock.releaseLock(token)
    }
}
