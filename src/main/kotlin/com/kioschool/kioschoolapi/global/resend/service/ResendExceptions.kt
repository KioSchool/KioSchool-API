package com.kioschool.kioschoolapi.global.resend.service

// 일일·월간 발송 한도를 다 썼을 때. 초당 호출 제한은 잠시 뒤 다시 시도하므로 여기에 들지 않는다.
class ResendDailyLimitException : RuntimeException("Resend 발송 한도 초과")

class ResendSendException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
