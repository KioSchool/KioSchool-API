package com.kioschool.kioschoolapi.product.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE, reason = "판매할 수 없는 상품입니다.")
class NotSellableProductException : Exception()