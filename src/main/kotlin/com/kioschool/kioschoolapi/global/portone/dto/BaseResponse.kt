package com.kioschool.kioschoolapi.global.portone.dto

class BaseResponse<T>(
    val code: Int,
    val message: String?,
    val response: T
)