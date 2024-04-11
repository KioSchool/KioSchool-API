package com.kioschool.kioschoolapi.user.dto

data class ResetPasswordRequestBody(
    val code: String,
    val password: String
)
