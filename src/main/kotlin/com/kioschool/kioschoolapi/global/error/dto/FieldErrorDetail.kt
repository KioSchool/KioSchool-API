package com.kioschool.kioschoolapi.global.error.dto

data class FieldErrorDetail(
    val field: String,
    val value: String?,
    val reason: String?,
)
