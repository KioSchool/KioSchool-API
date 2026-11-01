package com.kioschool.kioschoolapi.global.resend.service

class ResendDailyLimitException : RuntimeException("Resend 발송 한도 초과")

class ResendSendException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
