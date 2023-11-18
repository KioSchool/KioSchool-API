package com.kioschool.kioschoolapi.user.dto

data class RegisterRequestBody(
    val id: String,
    val password: String,
    val name: String,
    val email: String,
)