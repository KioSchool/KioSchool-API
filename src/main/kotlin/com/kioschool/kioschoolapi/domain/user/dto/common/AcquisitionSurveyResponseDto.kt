package com.kioschool.kioschoolapi.domain.user.dto.common

import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import java.time.LocalDateTime

data class AcquisitionSurveyResponseDto(
    val id: Long,
    val userId: Long,
    val userEmail: String?,
    val schoolName: String,
    val channel: AcquisitionChannel?,
    val channelLabel: String?,
    val channelEtc: String?,
    val context: String?,
    val answeredAt: LocalDateTime?,
    val user: SuperAdminUserDto
) {
    companion object {
        fun of(survey: AcquisitionSurvey, schoolName: String): AcquisitionSurveyResponseDto {
            return AcquisitionSurveyResponseDto(
                id = survey.id,
                userId = survey.user.id,
                userEmail = survey.user.email,
                schoolName = schoolName,
                channel = survey.channel,
                channelLabel = survey.channel?.label,
                channelEtc = survey.channelEtc,
                context = survey.context,
                answeredAt = survey.createdAt,
                user = SuperAdminUserDto.of(survey.user, schoolName)
            )
        }
    }
}
