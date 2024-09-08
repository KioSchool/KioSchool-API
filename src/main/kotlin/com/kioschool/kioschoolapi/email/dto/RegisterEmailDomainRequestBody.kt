package com.kioschool.kioschoolapi.email.dto

data class RegisterEmailDomainRequestBody(
    val name: String,
    val domain: String
)