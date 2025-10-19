package com.kioschool.kioschoolapi.domain.user.dto.request

import jakarta.validation.constraints.NotBlank

data class IsDuplicateLoginIdRequestBody(
    @field:NotBlank(message = "아이디는 필수 입력값입니다.")
    val id: String,
)