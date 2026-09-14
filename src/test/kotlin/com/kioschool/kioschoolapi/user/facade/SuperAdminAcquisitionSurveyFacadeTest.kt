package com.kioschool.kioschoolapi.user.facade

import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.domain.user.facade.SuperAdminAcquisitionSurveyFacade
import com.kioschool.kioschoolapi.domain.user.repository.AcquisitionSurveyRepository
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class SuperAdminAcquisitionSurveyFacadeTest : DescribeSpec({
    val userRepository = mockk<UserRepository>()
    val acquisitionSurveyRepository = mockk<AcquisitionSurveyRepository>()

    val sut = SuperAdminAcquisitionSurveyFacade(userRepository, acquisitionSurveyRepository)

    afterTest {
        clearAllMocks()
    }

    describe("getSummary") {
        it("channel이 null인 row는 건너뜀, row가 없는 유저는 미응답으로 센다") {
            every { userRepository.count() } returns 10L
            every { acquisitionSurveyRepository.countGroupByChannel() } returns listOf(
                arrayOf(AcquisitionChannel.INSTAGRAM, 3L),
                arrayOf(AcquisitionChannel.SENIOR_HANDOVER, 1L),
                arrayOf(null, 2L)
            )
            every { acquisitionSurveyRepository.countByContextIsNotNull() } returns 1L

            val result = sut.getSummary()

            result.answeredCount shouldBe 4L
            result.skippedCount shouldBe 2L
            result.notAskedCount shouldBe 4L
            result.responseRate shouldBe 4.0 / 6.0
            result.contextCount shouldBe 1L
        }

        it("응답이 없는 유입 경로도 0건으로 enum 순서대로 모두 내려준다") {
            every { userRepository.count() } returns 4L
            every { acquisitionSurveyRepository.countGroupByChannel() } returns listOf(
                arrayOf(AcquisitionChannel.INSTAGRAM, 3L),
                arrayOf(AcquisitionChannel.ETC, 1L)
            )
            every { acquisitionSurveyRepository.countByContextIsNotNull() } returns 0L

            val result = sut.getSummary()

            result.channels.map { it.channel } shouldBe AcquisitionChannel.entries.toList()
            result.channels.first { it.channel == AcquisitionChannel.INSTAGRAM }.ratio shouldBe 0.75
            result.channels.first { it.channel == AcquisitionChannel.SEARCH }.count shouldBe 0L
            result.channels.first { it.channel == AcquisitionChannel.SEARCH }.label shouldBe AcquisitionChannel.SEARCH.label
        }

        it("설문 row가 하나도 없으면 비율은 0이다") {
            every { userRepository.count() } returns 0L
            every { acquisitionSurveyRepository.countGroupByChannel() } returns emptyList()
            every { acquisitionSurveyRepository.countByContextIsNotNull() } returns 0L

            val result = sut.getSummary()

            result.responseRate shouldBe 0.0
            result.notAskedCount shouldBe 0L
            result.channels.all { it.ratio == 0.0 } shouldBe true
        }
    }

    describe("getResponses") {
        it("최신순으로 조회하고 이메일만 담는다") {
            val pageable = slot<Pageable>()
            val survey = AcquisitionSurvey(
                user = SampleEntity.user,
                channel = AcquisitionChannel.ETC,
                channelEtc = "에브리타임",
                context = "source=instagram&landing=/"
            )
            every { acquisitionSurveyRepository.findAllWithUser(AcquisitionChannel.ETC, capture(pageable)) } returns PageImpl(listOf(survey))

            val result = sut.getResponses(AcquisitionChannel.ETC, 0, 20)

            pageable.captured.sort.getOrderFor("createdAt")?.direction shouldBe Sort.Direction.DESC
            result.content.single().userEmail shouldBe SampleEntity.user.email
            result.content.single().channelLabel shouldBe AcquisitionChannel.ETC.label
            result.content.single().channelEtc shouldBe "에브리타임"
        }
    }
})
