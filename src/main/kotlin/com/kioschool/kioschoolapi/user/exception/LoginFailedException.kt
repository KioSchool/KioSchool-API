package com.kioschool.kioschoolapi.user.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "로그인에 실패하였습니다.")
class LoginFailedException : Exception()