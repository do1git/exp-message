package site.rahoon.message.monolithic.common.global.config

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.support.ContextPropagatingTaskDecorator
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableAsync
class AsyncConfig(
    private val builder: ThreadPoolTaskExecutorBuilder,
) {
    @Bean("taskExecutor")
    @Primary
    fun taskExecutor(): ThreadPoolTaskExecutor =
        builder
            .taskDecorator(ContextPropagatingTaskDecorator())
            .build()

    @Bean("taskScheduler")
    @Primary
    fun taskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 2
            setThreadNamePrefix("sched-")
            initialize()
        }
}
