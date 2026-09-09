package com.kioschool.kioschoolapi.domain.user.dto.admin

import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import org.hibernate.validator.constraints.Length

data class AcquisitionRequestBody(
    val channel: AcquisitionChannel? = null,
    @field:Length(max = 100, message = "유입 경로 직접 입력은 100자를 초과할 수 없습니다.")
    val channelEtc: String? = null,
    @field:Length(max = 500, message = "유입 문맥은 500자를 초과할 수 없습니다.")
    val context: String? = null,
)
