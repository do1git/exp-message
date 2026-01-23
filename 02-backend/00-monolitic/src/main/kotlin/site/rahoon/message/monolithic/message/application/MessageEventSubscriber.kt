package site.rahoon.message.monolithic.message.application

/**
 * 메시지 이벤트 구독 인터페이스 (Port)
 *
 * 필요한 곳에서 상속받아 구현하면 infrastructure 레이어에서 execute
 * 반환값은 Any?로 선언하여 구현체에서 자유롭게 반환할 수 있습니다.
 */
interface MessageEventSubscriber {
    /** 메시지 생성 이벤트 발생시 실행 */
    fun onCreated(event: MessageEvent.Created): Any?
}
