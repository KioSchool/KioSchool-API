package com.kioschool.kioschoolapi.account.dto

class RegisterAccountRequestBody(
    val bankId: Long,
    val accountNumber: String,
    val accountHolder: String,
)