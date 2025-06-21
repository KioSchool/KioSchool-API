package com.kioschool.kioschoolapi.domain.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class RegisterRequestBody(
    @field:NotBlank(message = "아이디는 필수 입력값입니다.")
    @field:Length(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    val id: String,
    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @field:Length(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    val password: String,
    @field:NotBlank(message = "이름은 필수 입력값입니다.")
    val name: String,
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
)