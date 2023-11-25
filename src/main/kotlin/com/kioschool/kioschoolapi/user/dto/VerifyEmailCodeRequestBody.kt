package com.kioschool.kioschoolapi.user.dto

data class VerifyEmailCodeRequestBody(
    val email: String,
    val code: String,
)