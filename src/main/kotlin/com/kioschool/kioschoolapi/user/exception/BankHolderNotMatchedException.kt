package com.kioschool.kioschoolapi.user.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY, reason = "예금주명이 일치하지 않습니다.")
class BankHolderNotMatchedException : Exception()