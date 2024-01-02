package com.kioschool.kioschoolapi.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterRequestBody(
    @field:NotBlank(message = "아이디는 필수 입력값입니다.")
    val id: String,
    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    val password: String,
    @field:NotBlank(message = "이름은 필수 입력값입니다.")
    val name: String,
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
)