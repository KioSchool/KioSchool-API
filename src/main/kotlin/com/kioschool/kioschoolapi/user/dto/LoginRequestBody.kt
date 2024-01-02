package com.kioschool.kioschoolapi.user.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequestBody(
    @field:NotBlank(message = "아이디는 필수 입력값입니다.")
    val id: String,
    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    val password: String
)