package site.rahoon.message.monolithic.common.test

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

/**
 * 동시 실행 테스트 유틸리티
 */
object ConcurrentTestUtils {
    /**
     * 여러 스레드에서 동시에 동일한 작업을 실행하고 결과를 수집합니다.
     *
     * @param threadCount 동시에 실행할 스레드 수
     * @param timeoutSecond 타임아웃 시간 (초)
     * @param action 각 스레드에서 실행할 작업
     * @return 각 스레드의 실행 결과 리스트
     */
    fun <T> executeConcurrent(
        threadCount: Int,
        timeoutSecond: Long = 30,
        action: () -> T,
    ): List<T> {
        val results = Collections.synchronizedList(mutableListOf<T>())
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) {
            executor.submit {
                try {
                    latch.countDown()
                    latch.await()
                    results.add(action())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        executor.shutdown()
        assertTrue(executor.awaitTermination(timeoutSecond, TimeUnit.SECONDS), "모든 스레드가 ${timeoutSecond}초 내에 완료되어야 합니다")
        return results
    }
}
