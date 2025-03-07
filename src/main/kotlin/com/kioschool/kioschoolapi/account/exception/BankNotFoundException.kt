package com.kioschool.kioschoolapi.account.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "은행을 찾을 수 없습니다.")
class BankNotFoundException : Exception()