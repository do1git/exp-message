package site.rahoon.message.monolithic.user.domain

/**
 * 시스템 레벨 사용자 역할
 */
enum class UserRole(
    val code: String,
) {
    ADMIN("ADMIN"),
    USER("USER"),
    ;

    companion object {
        fun fromCode(code: String): UserRole =
            entries.find { it.code == code }
                ?: USER // 기본값
    }
}
