package com.kioschool.kioschoolapi.domain.order.exception

import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(
    value = org.springframework.http.HttpStatus.BAD_REQUEST,
    reason = "테이블이 사용상태가 아니라 주문을 받을 수 없습니다. 주점 관리자에게 문의하세요."
)
class NoOrderSessionException : Exception()