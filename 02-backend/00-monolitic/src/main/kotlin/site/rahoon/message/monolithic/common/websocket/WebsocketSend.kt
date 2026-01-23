package site.rahoon.message.monolithic.common.websocket

/**
 * WebSocket 메시지 전송을 위한 어노테이션
 *
 * 메서드에 이 어노테이션을 추가하면 해당 메서드가 반환하는 값을
 * 지정된 WebSocket 토픽으로 브로드캐스트합니다.
 *
 * @param value WebSocket 토픽 경로 (예: "/topic/chat-rooms/{chatRoomId}/messages")
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebsocketSend(
    val value: String,
)
