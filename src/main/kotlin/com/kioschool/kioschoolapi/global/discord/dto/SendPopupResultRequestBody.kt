package com.kioschool.kioschoolapi.global.discord.dto

import jakarta.validation.constraints.Size

class SendPopupResultRequestBody(
    @field:Size(max = 1000, message = "팝업 결과는 1000자를 초과할 수 없습니다.")
    val result: String,
)
