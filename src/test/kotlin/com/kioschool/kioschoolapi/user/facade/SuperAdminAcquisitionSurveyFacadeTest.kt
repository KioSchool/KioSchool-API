package com.kioschool.kioschoolapi.user.facade

import com.kioschool.kioschoolapi.domain.email.entity.EmailDomain
import com.kioschool.kioschoolapi.domain.email.repository.EmailDomainRepository
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
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class SuperAdminAcquisitionSurveyFacadeTest : DescribeSpec({
    val userRepository = mockk<UserRepository>()
    val acquisitionSurveyRepository = mockk<AcquisitionSurveyRepository>()
    val emailDomainRepository = mockk<EmailDomainRepository>()

    val sut = SuperAdminAcquisitionSurveyFacade(userRepository, acquisitionSurveyRepository, emailDomainRepository)

    beforeTest {
        every { emailDomainRepository.findAll() } returns listOf(
            EmailDomain(name = "건국대학교", domain = "konkuk.ac.kr"),
            EmailDomain(name = "고려대학교", domain = "korea.ac.kr"),
            EmailDomain(name = "고려대학교", domain = "korea.edu")
        )
        every { acquisitionSurveyRepository.countByContextIsNotNull() } returns 0L
    }

    afterTest {
        clearAllMocks()
    }

    describe("getSummary") {
        it("channel이 null인 row는 건너뜀, row가 없는 유저는 미응답으로 센다") {
            every { userRepository.findAllEmails() } returns (1..10).map { "user$it@konkuk.ac.kr" }
            every { acquisitionSurveyRepository.findAllEmailAndChannel() } returns listOf(
                arrayOf("user1@konkuk.ac.kr", AcquisitionChannel.INSTAGRAM),
                arrayOf("user2@konkuk.ac.kr", AcquisitionChannel.INSTAGRAM),
                arrayOf("user3@konkuk.ac.kr", AcquisitionChannel.INSTAGRAM),
                arrayOf("user4@konkuk.ac.kr", AcquisitionChannel.SENIOR_HANDOVER),
                arrayOf("user5@konkuk.ac.kr", null),
                arrayOf("user6@konkuk.ac.kr", null)
            )
            every { acquisitionSurveyRepository.countByContextIsNotNull() } returns 1L

            val result = sut.getSummary()

            result.totalUsers shouldBe 10L
            result.answeredCount shouldBe 4L
            result.skippedCount shouldBe 2L
            result.notAskedCount shouldBe 4L
            result.surveyedRate shouldBe 0.6
            result.contextCount shouldBe 1L
        }

        it("응답이 없는 유입 경로도 0건으로 enum 순서대로 모두 내려준다") {
            every { userRepository.findAllEmails() } returns (1..4).map { "user$it@konkuk.ac.kr" }
            every { acquisitionSurveyRepository.findAllEmailAndChannel() } returns listOf(
                arrayOf("user1@konkuk.ac.kr", AcquisitionChannel.INSTAGRAM),
                arrayOf("user2@konkuk.ac.kr", AcquisitionChannel.INSTAGRAM),
                arrayOf("user3@konkuk.ac.kr", AcquisitionChannel.INSTAGRAM),
                arrayOf("user4@konkuk.ac.kr", AcquisitionChannel.ETC)
            )

            val result = sut.getSummary()

            result.channels.map { it.channel } shouldBe AcquisitionChannel.entries.toList()
            result.channels.first { it.channel == AcquisitionChannel.INSTAGRAM }.ratio shouldBe 0.75
            result.channels.first { it.channel == AcquisitionChannel.SEARCH }.count shouldBe 0L
            result.channels.first { it.channel == AcquisitionChannel.SEARCH }.label shouldBe AcquisitionChannel.SEARCH.label
        }

        it("설문 row가 하나도 없으면 비율은 0이다") {
            every { userRepository.findAllEmails() } returns emptyList()
            every { acquisitionSurveyRepository.findAllEmailAndChannel() } returns emptyList()

            val result = sut.getSummary()

            result.surveyedRate shouldBe 0.0
            result.notAskedCount shouldBe 0L
            result.channels.all { it.ratio == 0.0 } shouldBe true
            result.schools shouldBe emptyList()
        }

        it("이메일 도메인으로 학교를 묶고, 도메인이 여러 개인 학교는 합치고, 등록되지 않은 도메인은 도메인 그대로 쓴다") {
            every { userRepository.findAllEmails() } returns listOf(
                "a@korea.ac.kr",
                "b@korea.edu",
                "c@korea.ac.kr",
                "d@konkuk.ac.kr",
                "e@unknown.ac.kr",
                "f@unknown.ac.kr"
            )
            every { acquisitionSurveyRepository.findAllEmailAndChannel() } returns listOf(
                arrayOf("a@korea.ac.kr", AcquisitionChannel.SENIOR_HANDOVER),
                arrayOf("b@korea.edu", AcquisitionChannel.SAME_SCHOOL),
                arrayOf("c@korea.ac.kr", null),
                arrayOf("d@konkuk.ac.kr", AcquisitionChannel.INSTAGRAM)
            )

            val result = sut.getSummary()

            result.schools.map { it.schoolName } shouldBe listOf("고려대학교", "건국대학교", "unknown.ac.kr")

            val korea = result.schools.first { it.schoolName == "고려대학교" }
            korea.totalUsers shouldBe 3L
            korea.answeredCount shouldBe 2L
            korea.skippedCount shouldBe 1L
            korea.channels.map { it.channel } shouldBe AcquisitionChannel.entries.toList()
            korea.channels.first { it.channel == AcquisitionChannel.SENIOR_HANDOVER }.ratio shouldBe 0.5

            val unknown = result.schools.first { it.schoolName == "unknown.ac.kr" }
            unknown.totalUsers shouldBe 2L
            unknown.answeredCount shouldBe 0L
            unknown.channels.all { it.ratio == 0.0 } shouldBe true
        }
    }

    describe("getResponses") {
        it("최신순으로 조회하고 이메일과 학교명만 담는다") {
            val pageable = slot<Pageable>()
            val survey = AcquisitionSurvey(
                user = SampleEntity.user,
                channel = AcquisitionChannel.ETC,
                channelEtc = "에브리타임",
                context = "source=instagram&landing=/"
            )
            every { acquisitionSurveyRepository.findAllWithUser(AcquisitionChannel.ETC, capture(pageable)) } returns PageImpl(listOf(survey))

            val result = sut.getResponses(AcquisitionChannel.ETC, null, 0, 20)

            pageable.captured.sort.getOrderFor("createdAt")?.direction shouldBe Sort.Direction.DESC
            result.content.single().userEmail shouldBe SampleEntity.user.email
            result.content.single().schoolName shouldBe "test.com"
            result.content.single().channelLabel shouldBe AcquisitionChannel.ETC.label
            result.content.single().channelEtc shouldBe "에브리타임"
        }

        it("학교를 주면 그 학교의 모든 이메일 도메인으로 조회한다") {
            every { acquisitionSurveyRepository.findAllWithUserByEmailDomains(null, any(), any()) } returns PageImpl(emptyList())

            sut.getResponses(null, "고려대학교", 0, 20)

            verify { acquisitionSurveyRepository.findAllWithUserByEmailDomains(null, setOf("korea.ac.kr", "korea.edu"), any()) }
        }

        it("등록되지 않은 도메인이 학교명이면 그 도메인으로 조회한다") {
            every { acquisitionSurveyRepository.findAllWithUserByEmailDomains(null, any(), any()) } returns PageImpl(emptyList())

            sut.getResponses(null, "unknown.ac.kr", 0, 20)

            verify { acquisitionSurveyRepository.findAllWithUserByEmailDomains(null, setOf("unknown.ac.kr"), any()) }
        }
    }
})
