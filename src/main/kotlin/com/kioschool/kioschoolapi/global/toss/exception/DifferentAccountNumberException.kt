package com.kioschool.kioschoolapi.global.toss.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "토스 계좌와 사용자 계좌가 일치하지 않습니다.")
class DifferentAccountNumberException : Exception()