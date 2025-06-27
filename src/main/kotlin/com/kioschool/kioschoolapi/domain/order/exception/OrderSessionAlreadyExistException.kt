package com.kioschool.kioschoolapi.domain.order.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(
    value = HttpStatus.BAD_REQUEST,
    reason = "이미 테이블이 사용상태입니다."
)
class OrderSessionAlreadyExistException : Exception()