package site.rahoon.message.monolithic.loginfailure.domain

import java.time.Duration

/**
 * 로그인 실패 횟수 저장소 인터페이스
 */
interface LoginFailureRepository {
    /**
     * LoginFailure를 조회합니다.
     * @param key Redis 키 (이메일 또는 IP 주소)
     * @return LoginFailure (없으면 실패 횟수 0인 객체)
     */
    fun findByKey(key: String): LoginFailure

    /**
     * LoginFailure를 조회합니다.
     * @param key Redis 키 (이메일 또는 IP 주소)
     * @return LoginFailure (없으면 실패 횟수 0인 객체)
     */
    fun findByKeys(keys: List<String>): List<LoginFailure>

    /**
     * LoginFailure를 저장합니다.
     * @param loginFailure 저장할 LoginFailure
     * @param ttl TTL (Time To Live)
     * @return 저장된 LoginFailure
     */
    fun save(
        loginFailure: LoginFailure,
        ttl: Duration,
    ): LoginFailure

    /**
     * LoginFailure를 삭제합니다.
     * @param key Redis 키 (이메일 또는 IP 주소)
     */
    fun deleteByKey(key: String)

    /**
     * 실패 횟수를 원자적으로 증가시키고 증가된 값을 반환합니다.
     * @param key Redis 키 (이메일 또는 IP 주소)
     * @param ttl TTL (Time To Live)
     * @return 증가된 실패 횟수
     */
    fun incrementAndGet(
        key: String,
        ttl: Duration,
    ): Int

    /**
     * 여러 키의 실패 횟수를 원자적으로 증가시키고 증가된 값을 반환합니다.
     * @param keyTtlPairs 키와 TTL의 쌍 리스트
     * @return 증가된 실패 횟수 리스트 (입력 순서와 동일)
     */
    fun incrementAndGetMultiple(keyTtlPairs: List<Pair<String, Duration>>): List<LoginFailure>

    /**
     * 실패 횟수를 원자적으로 증가시키고 반환합니다.
     * 하나라도 limit 이상이면 업데이트하지 않고, 모두 limit 미만일 때만 증가시킵니다.
     * @param keyLimitTtlTriplet 키, 최대 실패 횟수, TTL의 트리플 리스트
     * @return Pair<Boolean, List<LoginFailure>> - 첫 번째 값은 업데이트 여부(true면 업데이트됨, false면 limit 이상으로 업데이트 안 됨), 두 번째 값은 업데이트전 LoginFailure 리스트 (입력 순서와 동일)
     */
    fun getAndIncrement(keyLimitTtlTriplet: List<Triple<String, Int, Duration>>): Pair<Boolean, List<LoginFailure>>
}
