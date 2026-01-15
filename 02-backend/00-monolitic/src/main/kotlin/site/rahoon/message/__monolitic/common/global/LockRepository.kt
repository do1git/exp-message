package site.rahoon.message.__monolitic.common.global

import java.time.Duration

/**
 * 분산 락(Distributed Lock) 저장소 인터페이스
 * LockToken 기반으로 자신이 획득한 락만 해제할 수 있습니다.
 */
interface LockRepository {
    /**
     * 락을 획득합니다.
     * @param key 락 키
     * @param ttl 락 만료 시간 (Time To Live)
     * @return LockToken (성공 시), null (실패 시)
     */
    fun acquireLock(key: String, ttl: Duration): LockToken?

    /**
     * 여러 키에 대해 락을 획득합니다.
     * 모든 락을 획득하거나, 하나라도 실패하면 모두 롤백합니다.
     * @param keys 락 키 리스트
     * @param ttl 락 만료 시간
     * @return LockToken (성공 시), null (실패 시)
     */
    fun acquireLocks(keys: List<String>, ttl: Duration): LockToken?

    /**
     * 락을 해제합니다. 토큰에 포함된 정보로 자신의 락만 해제합니다.
     * @param token 락 획득 시 받은 토큰
     * @return 락 해제 성공 여부
     */
    fun releaseLock(token: LockToken): Boolean
}
