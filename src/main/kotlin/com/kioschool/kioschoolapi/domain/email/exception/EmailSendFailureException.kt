package com.kioschool.kioschoolapi.domain.email.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.METHOD_FAILURE, reason = "이메일 전송에 실패했습니다.")
class EmailSendFailureException : Exception()