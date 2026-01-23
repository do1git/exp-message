package site.rahoon.message.monolithic.common.global

import jakarta.annotation.PostConstruct
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * 비동기 실행을 위한 헬퍼 클래스
 * 별도의 스레드 풀에서 코드를 실행하도록 간소화하는 유틸리티 메소드 제공
 */
@Component
class AsyncRunner(
    private val threadPoolMap: Map<String, Executor>,
) {
    companion object {
        @Volatile
        lateinit var inst: AsyncRunner
            private set

        const val DEFAULT_THREAD_POOL = "taskExecutor"

        /**
         * 반환값이 없는 비동기 작업 실행
         *
         * 예외가 발생할 경우 자연 소멸 (AsyncConfig의 에러 처리 로직에 따라 보고됨)
         */
        fun runAsync(
            threadPool: String = DEFAULT_THREAD_POOL,
            action: () -> Unit,
        ) {
            inst.runAsync(threadPool, action)
        }

        /**
         * 반환값이 없는 비동기 작업 실행
         *
         * 예외가 발생할 경우 CompletableFuture 에 담김 (직접 처리해야함)
         */
        fun <T> supplyAsync(
            threadPool: String = DEFAULT_THREAD_POOL,
            action: () -> T,
        ): CompletableFuture<T> = inst.supplyAsync(threadPool, action)

        // TODO 코루틴 메소드 추가
//        /** 코루틴을 비동기로 실행 (결과값 필요 없을 때) */
//        fun launch(threadPool: String = DEFAULT_THREAD_POOL, block: suspend CoroutineScope.() -> Unit): Job {
//            return inst.launch(threadPool, block)
//        }
//        /** 코루틴을 비동기로 실행하고 결과를 Deferred로 반환 (await 가능) */
//        fun <T> async(threadPool: String = DEFAULT_THREAD_POOL, block: suspend CoroutineScope.() -> T): Deferred<T> {
//            return inst.async(threadPool, block)
//        }
    }

    @PostConstruct
    fun init() {
        inst = this
    }

    fun getThreadPoolNames(): List<String> = threadPoolMap.keys.toList()

    fun runAsync(
        threadPool: String = DEFAULT_THREAD_POOL,
        action: () -> Unit,
    ) {
        val executor = threadPoolMap[threadPool] ?: throw IllegalArgumentException("ThreadPool not found: $threadPool")
        executor.execute { action() }
    }

    fun <T> supplyAsync(
        threadPool: String = DEFAULT_THREAD_POOL,
        action: () -> T,
    ): CompletableFuture<T> {
        val executor = threadPoolMap[threadPool] ?: throw IllegalArgumentException("ThreadPool not found: $threadPool")
        if (executor !is ThreadPoolTaskExecutor) throw IllegalArgumentException("ThreadPool does not support submit: $threadPool")
        return CompletableFuture<T>.runAsync({ action() }, executor) as CompletableFuture<T>
    }

//    // TODO 코루틴 구현
//    private val dispatcherMap = threadPoolMap.mapValues { it.value.asCoroutineDispatcher() }
//    fun launch(threadPool: String, block: suspend CoroutineScope.() -> Unit): Job {
//        val dispatcher = dispatcherMap[threadPool] ?: throw IllegalArgumentException("Dispatcher not found: $threadPool")
//        return CoroutineScope(dispatcher + SupervisorJob()).launch { block() }
//    }
//    fun <T> async(threadPool: String, block: suspend CoroutineScope.() -> T): Deferred<T> {
//        val dispatcher = dispatcherMap[threadPool] ?: throw IllegalArgumentException("Dispatcher not found: $threadPool")
//        return CoroutineScope(dispatcher + SupervisorJob()).async { block() }
//    }
}
