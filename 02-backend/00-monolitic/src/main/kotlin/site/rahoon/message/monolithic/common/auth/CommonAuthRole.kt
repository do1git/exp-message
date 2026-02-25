package site.rahoon.message.monolithic.common.auth

/**
 * 인증 컨텍스트에서 사용하는 시스템 레벨 역할.
 *
 * [CommonAuthInfo]의 role 필드 타입으로 사용된다.
 */
enum class CommonAuthRole(
    val code: String,
) {
    ADMIN("ADMIN"),
    USER("USER"),
    ;

    companion object {
        fun fromCode(code: String): CommonAuthRole =
            entries.find { it.code == code }
                ?: USER // 기본값
    }
}
