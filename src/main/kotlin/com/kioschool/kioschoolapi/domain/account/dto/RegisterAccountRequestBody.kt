package com.kioschool.kioschoolapi.domain.account.dto

class RegisterAccountRequestBody(
    val bankId: Long,
    val accountNumber: String,
    val accountHolder: String,
)