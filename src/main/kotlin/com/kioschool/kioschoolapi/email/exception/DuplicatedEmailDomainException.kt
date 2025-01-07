package com.kioschool.kioschoolapi.email.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "이미 등록된 도메인입니다.")
class DuplicatedEmailDomainException : Exception()