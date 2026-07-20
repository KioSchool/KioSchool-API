package com.kioschool.kioschoolapi.global.error.exception

import com.kioschool.kioschoolapi.global.error.ErrorCode

class CustomException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message ?: errorCode.defaultMessage, cause)
