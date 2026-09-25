package com.kioschool.kioschoolapi.domain.user.facade

import com.kioschool.kioschoolapi.domain.email.repository.EmailDomainRepository
import com.kioschool.kioschoolapi.domain.email.service.SchoolResolver
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
    private val acquisitionSurveyRepository: AcquisitionSurveyRepository,
    private val emailDomainRepository: EmailDomainRepository
) {
    fun getSummary(): AcquisitionSurveySummaryDto {
        val schoolResolver = createSchoolResolver()
        val userEmails = userRepository.findAllEmails()
        // 건너뛴 응답은 channel = null인 row로 남는다.
        val surveyChannels = acquisitionSurveyRepository.findAllEmailAndChannel()
            .map { row -> row[0] as String? to row[1] as AcquisitionChannel? }

        val totalUsers = userEmails.size.toLong()
        val overall = tally(surveyChannels.map { it.second })
        val surveyedCount = overall.answeredCount + overall.skippedCount

        val userCountBySchool = userEmails.groupingBy(schoolResolver::schoolOf).eachCount()
        val channelsBySchool = surveyChannels.groupBy({ schoolResolver.schoolOf(it.first) }, { it.second })
        val schools = userCountBySchool
            .map { (schoolName, userCount) ->
                val schoolTally = tally(channelsBySchool[schoolName].orEmpty())
                AcquisitionSurveySummaryDto.SchoolStat(
                    schoolName = schoolName,
                    totalUsers = userCount.toLong(),
                    answeredCount = schoolTally.answeredCount,
                    skippedCount = schoolTally.skippedCount,
                    channels = schoolTally.channels
                )
            }
            .sortedWith(
                compareByDescending<AcquisitionSurveySummaryDto.SchoolStat> { it.answeredCount }
                    .thenByDescending { it.totalUsers }
                    .thenBy { it.schoolName }
            )

        return AcquisitionSurveySummaryDto(
            totalUsers = totalUsers,
            answeredCount = overall.answeredCount,
            skippedCount = overall.skippedCount,
            notAskedCount = (totalUsers - surveyedCount).coerceAtLeast(0),
            surveyedRate = ratioOf(surveyedCount, totalUsers),
            contextCount = acquisitionSurveyRepository.countByContextIsNotNull(),
            channels = overall.channels,
            schools = schools
        )
    }

    fun getResponses(
        channel: AcquisitionChannel?,
        school: String?,
        page: Int,
        size: Int
    ): Page<AcquisitionSurveyResponseDto> {
        val schoolResolver = createSchoolResolver()
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val surveys = if (school.isNullOrBlank()) {
            acquisitionSurveyRepository.findAllWithUser(channel, pageable)
        } else if (school == NO_EMAIL_SCHOOL_NAME) {
            acquisitionSurveyRepository.findAllWithUserWithoutEmail(channel, pageable)
        } else {
            acquisitionSurveyRepository.findAllWithUserByEmailDomains(
                channel,
                schoolResolver.domainsOf(school),
                pageable
            )
        }
        return surveys.map { AcquisitionSurveyResponseDto.of(it, schoolResolver.schoolOf(it.user.email)) }
    }

    private fun createSchoolResolver(): SchoolResolver {
        return SchoolResolver.of(emailDomainRepository.findAll())
    }

    private fun tally(channels: List<AcquisitionChannel?>): ChannelTally {
        val countByChannel = channels.groupingBy { it }.eachCount()
        val skippedCount = (countByChannel[null] ?: 0).toLong()
        val answeredCount = channels.size - skippedCount
        val channelStats = AcquisitionChannel.entries.map { channel ->
            val count = (countByChannel[channel] ?: 0).toLong()
            AcquisitionSurveySummaryDto.ChannelStat(
                channel = channel,
                label = channel.label,
                count = count,
                ratio = ratioOf(count, answeredCount)
            )
        }
        return ChannelTally(answeredCount, skippedCount, channelStats)
    }

    private fun ratioOf(part: Long, whole: Long): Double =
        if (whole > 0) part.toDouble() / whole else 0.0

    private data class ChannelTally(
        val answeredCount: Long,
        val skippedCount: Long,
        val channels: List<AcquisitionSurveySummaryDto.ChannelStat>
    )

    companion object {
        const val NO_EMAIL_SCHOOL_NAME = SchoolResolver.NO_EMAIL_SCHOOL_NAME
    }
}
