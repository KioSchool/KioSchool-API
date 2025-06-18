package com.kioschool.kioschoolapi.domain.user.dto

data class SendResetPasswordEmailRequestBody(
    val id: String,
    val email: String
)