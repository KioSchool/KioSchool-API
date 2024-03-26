package com.kioschool.kioschoolapi.user.dto

data class SendResetPasswordEmailRequestBody(
    val id: String,
    val email: String
)