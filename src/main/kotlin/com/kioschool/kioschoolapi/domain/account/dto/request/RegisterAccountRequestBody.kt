package com.kioschool.kioschoolapi.domain.account.dto.request

class RegisterAccountRequestBody(
    val bankId: Long,
    val accountNumber: String,
    val accountHolder: String,
)