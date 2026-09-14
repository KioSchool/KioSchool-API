package com.kioschool.kioschoolapi.domain.user.dto.common

import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel

data class AcquisitionSurveySummaryDto(
    val totalUsers: Long,
    val answeredCount: Long,
    val skippedCount: Long,
    // 설문 배포 이전 가입자도 여기에 포함된다.
    val notAskedCount: Long,
    val responseRate: Double,
    val contextCount: Long,
    val channels: List<ChannelStat>
) {
    data class ChannelStat(
        val channel: AcquisitionChannel,
        val label: String,
        val count: Long,
        val ratio: Double
    )
}
