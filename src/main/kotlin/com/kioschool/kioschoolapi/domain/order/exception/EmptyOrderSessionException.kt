package com.kioschool.kioschoolapi.domain.order.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(
    value = HttpStatus.BAD_REQUEST,
    reason = "예기치 않게 주문 내역이 없는 세션입니다. 세션 기록을 삭제하시겠습니까, 아니면 이대로 저장하시겠습니까?"
)
class EmptyOrderSessionException : Exception()
