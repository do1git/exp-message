package site.rahoon.message.monolithic.message.application

/**
 * 메시지 이벤트 발행 인터페이스 (Port)
 *
 * 실제 구현은 infrastructure 레이어에서 제공 (Redis Pub/Sub, Kafka 등)
 */
interface MessageEventPublisher {
    /**
     * 메시지 생성 이벤트 발행
     */
    fun publishCreated(event: MessageEvent.Created)
}
