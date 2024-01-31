package com.kioschool.kioschoolapi.email.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY, reason = "허용되지 않은 이메일 도메인입니다.")
class NotVerifiedEmailDomainException : Exception()