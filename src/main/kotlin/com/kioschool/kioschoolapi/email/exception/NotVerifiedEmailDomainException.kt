package com.kioschool.kioschoolapi.email.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY, reason = "로그인에 실패하였습니다.")
class NotVerifiedEmailDomainException : Exception()