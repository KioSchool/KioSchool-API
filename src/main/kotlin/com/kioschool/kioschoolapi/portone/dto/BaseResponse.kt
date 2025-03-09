package com.kioschool.kioschoolapi.portone.dto

class BaseResponse<T>(
    val code: Int,
    val message: String?,
    val response: T
)