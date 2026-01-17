package site.rahoon.message.monolithic.common.domain

import site.rahoon.message.monolithic.common.global.ErrorType

interface DomainError {
    val code: String
    val message: String
    val type: ErrorType
}
