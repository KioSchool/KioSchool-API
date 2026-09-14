package com.kioschool.kioschoolapi.domain.user.facade

import com.kioschool.kioschoolapi.domain.user.dto.common.AcquisitionSurveyResponseDto
import com.kioschool.kioschoolapi.domain.user.dto.common.AcquisitionSurveySummaryDto
import com.kioschool.kioschoolapi.domain.user.repository.AcquisitionSurveyRepository
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class SuperAdminAcquisitionSurveyFacade(
    private val userRepository: UserRepository,
    private val acquisitionSurveyRepository: AcquisitionSurveyRepository
) {
    fun getSummary(): AcquisitionSurveySummaryDto {
        val totalUsers = userRepository.count()
        val countByChannel = acquisitionSurveyRepository.countGroupByChannel()
            .associate { row -> row[0] as AcquisitionChannel? to (row[1] as Number).toLong() }

        // 건너뛴 응답은 channel = null인 row로 남는다.
        val skippedCount = countByChannel[null] ?: 0L
        val answeredCount = countByChannel.filterKeys { it != null }.values.sum()
        val surveyedCount = answeredCount + skippedCount

        val channels = AcquisitionChannel.entries.map { channel ->
            val count = countByChannel[channel] ?: 0L
            AcquisitionSurveySummaryDto.ChannelStat(
                channel = channel,
                label = channel.label,
                count = count,
                ratio = ratioOf(count, answeredCount)
            )
        }

        return AcquisitionSurveySummaryDto(
            totalUsers = totalUsers,
            answeredCount = answeredCount,
            skippedCount = skippedCount,
            notAskedCount = (totalUsers - surveyedCount).coerceAtLeast(0),
            surveyedRate = ratioOf(surveyedCount, totalUsers),
            contextCount = acquisitionSurveyRepository.countByContextIsNotNull(),
            channels = channels
        )
    }

    fun getResponses(channel: AcquisitionChannel?, page: Int, size: Int): Page<AcquisitionSurveyResponseDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return acquisitionSurveyRepository.findAllWithUser(channel, pageable)
            .map { AcquisitionSurveyResponseDto.of(it) }
    }

    private fun ratioOf(part: Long, whole: Long): Double =
        if (whole > 0) part.toDouble() / whole else 0.0
}
