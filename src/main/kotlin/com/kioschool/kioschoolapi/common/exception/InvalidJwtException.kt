package com.kioschool.kioschoolapi.common.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "유효하지 않은 토큰입니다.")
class InvalidJwtException : Exception()