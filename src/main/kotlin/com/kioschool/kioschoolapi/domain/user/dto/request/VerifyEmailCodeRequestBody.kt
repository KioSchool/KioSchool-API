package com.kioschool.kioschoolapi.domain.user.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class VerifyEmailCodeRequestBody(
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
    @field:NotBlank(message = "인증 코드는 필수 입력값입니다.")
    val code: String,
)