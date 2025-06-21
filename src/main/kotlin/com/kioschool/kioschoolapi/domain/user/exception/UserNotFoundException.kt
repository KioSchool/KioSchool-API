package com.kioschool.kioschoolapi.domain.user.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "사용자를 찾을 수 없습니다.")
class UserNotFoundException : Exception()