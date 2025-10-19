package com.kioschool.kioschoolapi.domain.user.dto.request

data class SendResetPasswordEmailRequestBody(
    val id: String,
    val email: String
)