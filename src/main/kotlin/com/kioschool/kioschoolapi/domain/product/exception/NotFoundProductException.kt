package com.kioschool.kioschoolapi.domain.product.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "존재하지 않는 상품입니다.")
class NotFoundProductException : Exception()