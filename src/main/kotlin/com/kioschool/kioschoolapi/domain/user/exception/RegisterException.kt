package com.kioschool.kioschoolapi.domain.user.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "회원가입에 실패하였습니다.")
class RegisterException : Exception()