package site.rahoon.message.monolithic.common.global.config

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class AsyncConfig(
    private val builder: ThreadPoolTaskExecutorBuilder,
) {
    @Bean("taskExecutor")
    fun taskExecutor(): ThreadPoolTaskExecutor = builder.build()
}
