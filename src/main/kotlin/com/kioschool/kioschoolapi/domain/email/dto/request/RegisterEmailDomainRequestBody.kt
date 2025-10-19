package com.kioschool.kioschoolapi.domain.email.dto.request

data class RegisterEmailDomainRequestBody(
    val name: String,
    val domain: String
)