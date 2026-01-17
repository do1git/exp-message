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
}
