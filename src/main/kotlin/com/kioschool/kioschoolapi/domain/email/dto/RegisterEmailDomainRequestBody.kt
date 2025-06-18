package com.kioschool.kioschoolapi.domain.email.dto

data class RegisterEmailDomainRequestBody(
    val name: String,
    val domain: String
)