package com.kioschool.kioschoolapi.user.dto

import jakarta.validation.constraints.Email

data class SendEmailCodeRequestBody(
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
)