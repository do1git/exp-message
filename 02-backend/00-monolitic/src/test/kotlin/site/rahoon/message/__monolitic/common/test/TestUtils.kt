package site.rahoon.message.__monolitic.common.test

import java.util.UUID

/**
 * 테스트 공통 유틸리티
 */
object TestUtils {

    /**
     * 고유한 이메일을 생성합니다.
     *
     * @param prefix 이메일 앞에 붙는 접두사 (기본값: "test")
     * @return 고유한 이메일 주소
     */
    fun uniqueEmail(prefix: String = "test"): String =
        "$prefix-${UUID.randomUUID()}@example.com"

    /**
     * 테스트용 고유 IP 주소를 생성합니다.
     * 로그인 실패 시 IP 기반 차단을 우회하기 위해 사용합니다.
     *
     * @return 고유한 IP 주소 (10.x.x.x 형태)
     */
    fun uniqueIp(): String =
        "10.${(0..255).random()}.${(0..255).random()}.${(0..255).random()}"
}
