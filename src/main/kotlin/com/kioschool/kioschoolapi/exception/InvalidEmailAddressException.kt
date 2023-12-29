package com.kioschool.kioschoolapi.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY, reason = "이메일 주소가 올바르지 않습니다.")
class InvalidEmailAddressException : Exception()