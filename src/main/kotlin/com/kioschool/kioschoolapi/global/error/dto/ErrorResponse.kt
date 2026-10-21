package com.kioschool.kioschoolapi.global.error.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.kioschool.kioschoolapi.global.error.ErrorCode
import java.time.Instant

data class ErrorResponse(
    val code: String,
    val message: String,
    val status: Int,
    val path: String,
    val timestamp: Instant,
    val errors: List<FieldErrorDetail> = emptyList(),
    // 에러 코드마다 모양이 정해진 추가 정보. 없으면 응답에서 빠져 기존 에러 응답 모양이 그대로다.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val details: Map<String, Any>? = null,
) {
    companion object {
        fun of(
            errorCode: ErrorCode,
            path: String,
            message: String? = null,
            errors: List<FieldErrorDetail> = emptyList(),
            details: Map<String, Any>? = null,
        ) = ErrorResponse(
            code = errorCode.name,
            message = message ?: errorCode.defaultMessage,
            status = errorCode.status.value(),
            path = path,
            timestamp = Instant.now(),
            errors = errors,
            details = details,
        )
    }
}
