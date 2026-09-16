package com.kioschool.kioschoolapi.domain.user.dto.common

import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel

data class AcquisitionSurveySummaryDto(
    val totalUsers: Long,
    val answeredCount: Long,
    val skippedCount: Long,
    // 설문은 어드민 홈 진입 시 뜬다. 아직 홈에 오지 않았거나 보고 나간 유저가 여기에 잡힌다.
    val notAskedCount: Long,
    // 전체 유저 중 설문을 마친(응답 + 건너뜀) 비율
    val surveyedRate: Double,
    // context는 자유 서술이 아니라 첫 방문 시 자동 수집한 UTM·referrer 값이다.
    val contextCount: Long,
    val channels: List<ChannelStat>,
    // 학교는 가입 이메일 도메인으로 판별한다. email_domain에 없는 도메인은 도메인 문자열이 그대로 학교명이 된다.
    val schools: List<SchoolStat>
) {
    data class ChannelStat(
        val channel: AcquisitionChannel,
        val label: String,
        val count: Long,
        val ratio: Double
    )

    data class SchoolStat(
        val schoolName: String,
        val totalUsers: Long,
        val answeredCount: Long,
        val skippedCount: Long,
        val channels: List<ChannelStat>
    )
}
