package com.kioschool.kioschoolapi.domain.product.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.METHOD_NOT_ALLOWED, reason = "해당 카테고리를 사용하는 상품이 존재합니다.")
class CanNotDeleteUsingProductCategoryException : Exception()