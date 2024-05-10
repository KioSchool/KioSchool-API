package com.kioschool.kioschoolapi.user.dto

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class ResetPasswordRequestBody(
    @field:NotBlank(message = "인증 코드는 필수 입력값입니다.")
    val code: String,
    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @field:Length(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
    val password: String
)
