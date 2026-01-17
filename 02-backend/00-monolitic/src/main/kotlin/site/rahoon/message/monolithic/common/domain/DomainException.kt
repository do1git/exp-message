package site.rahoon.message.monolithic.common.domain

class DomainException(
    val error: DomainError,
    val details: Map<String, Any>? = null,
    cause: Throwable? = null,
) : RuntimeException(
        details?.let {
            val detailsStr = it.entries.joinToString(", ") { (k, v) -> "$k=$v" }
            "${error.message}: $detailsStr"
        } ?: error.message,
        cause,
    )
