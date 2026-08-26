package com.kioschool.kioschoolapi.global.error.dto

data class FieldErrorDetail(
    val field: String,
    val value: String?,
    val reason: String?,
    // field가 컬렉션 항목을 가리킬 때(예: "positions[1].position") 그 요소의 위치.
    // 프론트가 field 문자열을 파싱하지 않고도 문제가 된 항목을 짚을 수 있게 한다.
    val index: Int? = null,
)
