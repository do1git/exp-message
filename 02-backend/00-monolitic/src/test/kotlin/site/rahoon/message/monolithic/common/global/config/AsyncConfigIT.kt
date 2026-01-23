package site.rahoon.message.monolithic.common.global.config

import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import java.time.Duration

class AsyncConfigIT(
    private val asyncTestService: AsyncTestService,
) : IntegrationTestBase() {
    @Test
    fun `비동기_메소드는_다른_스레드에서_실행되어야_한다`() {
        // Given
        val mainThreadName = Thread.currentThread().name

        // When
        asyncTestService.asyncMethod()

        // Then
        await.atMost(Duration.ofSeconds(3)).until {
            // 메인 스레드와 실행 스레드 이름이 다르면 비동기 성공!
            asyncTestService.lastExecutionThreadName != null &&
                asyncTestService.lastExecutionThreadName != mainThreadName
        }

        println("Main: $mainThreadName, Async: ${asyncTestService.lastExecutionThreadName}")
    }

    @Test
    fun `동기_메소드는_메인_스레드에서_실행되어야_한다`() {
        val mainThreadName = Thread.currentThread().name

        asyncTestService.syncMethod()

        // 즉시 검증 가능
        asyncTestService.lastExecutionThreadName shouldBe mainThreadName
    }

    @Test
    fun `비동기_메소드_예외는_메인메소드에_영향을_끼치면 안된다`() {
        // Given
        val mainThreadName = Thread.currentThread().name

        // When
        asyncTestService.asyncThrowMethod()

        // Then
        await.atMost(Duration.ofSeconds(3)).until {
            // 메인 스레드와 실행 스레드 이름이 다르면 비동기 성공!
            asyncTestService.lastExecutionThreadName != null &&
                asyncTestService.lastExecutionThreadName != mainThreadName
        }

        println("Main: $mainThreadName, Async: ${asyncTestService.lastExecutionThreadName}")
    }
}

@Service
class AsyncTestService {
    // 스레드 이름을 저장할 스레드 안전한 변수
    var lastExecutionThreadName: String? = null

    @Async
    fun asyncMethod() {
        lastExecutionThreadName = Thread.currentThread().name
    }

    fun syncMethod() {
        lastExecutionThreadName = Thread.currentThread().name
    }

    @Async
    fun asyncThrowMethod() {
        lastExecutionThreadName = Thread.currentThread().name
        throw RuntimeException("이슈발생")
    }
}
