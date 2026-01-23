package site.rahoon.message.monolithic.common.global

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.common.test.IntegrationTestBase

class AsyncRunnerIT(
    private val asyncRunner: AsyncRunner,
) : IntegrationTestBase() {
    @Test
    fun getThreadPoolNames() {
        val threadPoolNames = asyncRunner.getThreadPoolNames()
        println(threadPoolNames.joinToString(separator = "\n"))
    }

    @Test
    fun runAsyncException() {
        asyncRunner.runAsync { throw RuntimeException("예외발생") }
    }

    @Test
    fun supplyAsyncException() {
        val future = asyncRunner.supplyAsync { throw RuntimeException("예외발생") }

        val exception = shouldThrow<RuntimeException> {
            future.join()
        }

        exception.cause?.message shouldBe "예외발생"
    }
}
