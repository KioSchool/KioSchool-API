package com.kioschool.kioschoolapi.global.error.exception

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.dto.FieldErrorDetail

class CustomException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
    // 응답 errors[]에 그대로 실린다. 프론트가 문제가 된 입력 지점을 짚어줄 수 있도록
    // 도메인 예외에서도 필드 단위 상세를 붙일 수 있게 한다.
    val errors: List<FieldErrorDetail> = emptyList(),
    // 응답 details에 그대로 실린다. 모양은 에러 코드마다 정해져 있다.
    val details: Map<String, Any>? = null,
) : RuntimeException(message ?: errorCode.defaultMessage, cause)
