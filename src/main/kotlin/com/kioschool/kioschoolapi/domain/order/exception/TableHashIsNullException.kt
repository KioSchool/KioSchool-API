package com.kioschool.kioschoolapi.domain.order.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(
    value = HttpStatus.BAD_REQUEST,
    reason = "주문 URL이 유효하지 않습니다. 주점 관리자에게 문의하세요."
)
class TableHashIsNullException : Exception()