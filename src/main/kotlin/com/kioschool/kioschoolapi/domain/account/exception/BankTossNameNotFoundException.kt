package com.kioschool.kioschoolapi.domain.account.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "해당 은행은 토스 명칭이 등록되어 있지 않습니다.")
class BankTossNameNotFoundException : Exception()
