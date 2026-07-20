package com.kioschool.kioschoolapi.global.error.dto

import com.kioschool.kioschoolapi.global.error.ErrorCode
import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int,
    val path: String,
    val timestamp: Instant,
    val errors: List<FieldErrorDetail> = emptyList(),
) {
    companion object {
        fun of(
            errorCode: ErrorCode,
            path: String,
            message: String? = null,
            errors: List<FieldErrorDetail> = emptyList(),
        ) = ErrorResponse(
            code = errorCode.name,
            message = message ?: errorCode.defaultMessage,
            status = errorCode.status.value(),
            path = path,
            timestamp = Instant.now(),
            errors = errors,
        )
    }
}
