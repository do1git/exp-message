package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * WebSocket SimpleBroker heartbeat 전용 TaskScheduler.
 *
 * - @Scheduled 메서드용 taskScheduler는 AsyncConfig에 정의됨
 */
@Configuration
class WebSocketTaskSchedulerConfig {
    @Bean
    fun webSocketBrokerTaskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("ws-hb-")
            initialize()
        }
}
