package com.kioschool.kioschoolapi.domain.user.dto.admin

import com.kioschool.kioschoolapi.global.logging.annotation.Masked
import jakarta.validation.constraints.NotBlank

data class WithdrawRequestBody(
    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Masked
    val password: String
)
