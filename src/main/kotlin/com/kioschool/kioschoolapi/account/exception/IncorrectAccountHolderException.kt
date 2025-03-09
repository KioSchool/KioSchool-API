package com.kioschool.kioschoolapi.account.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "예금주명이 일치하지 않습니다.")
class IncorrectAccountHolderException : Exception()