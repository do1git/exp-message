package site.rahoon.message.__monolitic.common.domain

import site.rahoon.message.__monolitic.common.global.ErrorType

interface DomainError {
    val code: String
    val message: String
    val type: ErrorType
}