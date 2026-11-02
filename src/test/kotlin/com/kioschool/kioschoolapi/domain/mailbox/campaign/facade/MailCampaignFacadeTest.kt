package com.kioschool.kioschoolapi.domain.mailbox.campaign.facade

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceFilter
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceRow
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignPreviewRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailCampaignRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaign
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaignRecipient
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailCampaignStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRecipientRepository
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRepository
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailAudienceService
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailAudienceStats
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignComposer
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignSender
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignService
import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailTemplateRenderer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailBodyRenderer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailUnsubscribeService
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

class MailCampaignFacadeTest : DescribeSpec({
    val campaignRepository = mockk<MailCampaignRepository>()
    val recipientRepository = mockk<MailCampaignRecipientRepository>()
    val workspaceRepository = mockk<WorkspaceRepository>()
    val audience = mockk<MailAudienceService>()
    val sender = mockk<MailCampaignSender>(relaxed = true)
    val userService = mockk<UserService>()
    val unsubscribe = mockk<MailUnsubscribeService>()
    val client = mockk<ResendInboxClient>()
    val bodyRenderer = mockk<MailBodyRenderer>()
    val sanitizer = MailHtmlSanitizer()
    val renderer = MailTemplateRenderer(sanitizer)
    val mapper = ObjectMapper().findAndRegisterModules()
    val service = MailCampaignService(campaignRepository, recipientRepository, workspaceRepository, mapper, sanitizer, 365)
    val composer = MailCampaignComposer(renderer, bodyRenderer, "https://kio-school.com", "https://api.kio-school.com")

    fun sut() = MailCampaignFacade(service, audience, renderer, composer, sender, userService, unsubscribe, client, 70)

    val filter = MailAudienceFilter(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31), setOf(1L, 2L))
    val body = MailCampaignRequestBody(filter, "{주점 이름} 안내", "<p>{사장 이름}님</p>", 2, "req-1")
    val rows = listOf(MailAudienceRow(1, "달빛", "건국대학교", "김사장", "a@konkuk.ac.kr"), MailAudienceRow(2, "별빛", "건국대학교", "이사장", "b@konkuk.ac.kr"))
    val appender = ListAppender<ILoggingEvent>().apply { start() }
    (LoggerFactory.getLogger(MailCampaignFacade::class.java) as Logger).addAppender(appender)

    fun campaign(status: MailCampaignStatus = MailCampaignStatus.SENDING) = MailCampaign(
        subject = "s", bodyTemplate = "b", filterJson = "{}", createdBy = null,
        purgeAt = java.time.LocalDateTime.of(2027, 1, 1, 0, 0), status = status,
    )

    beforeTest {
        clearAllMocks()
        appender.list.clear()
        every { userService.getUser("admin") } returns SampleEntity.user
        every { audience.resolve(filter) } returns rows
        every { campaignRepository.save(any<MailCampaign>()) } answers { firstArg() }
        every { campaignRepository.findByRequestId(any()) } returns null
        every { campaignRepository.findById(any()) } returns java.util.Optional.of(campaign())
        every { recipientRepository.saveAll(any<List<MailCampaignRecipient>>()) } answers { firstArg() }
        every { recipientRepository.countByCampaignGroupByStatus(any()) } returns emptyList()
        every { workspaceRepository.getReferenceById(any()) } returns SampleEntity.workspace
        every { bodyRenderer.render(any(), any()) } returns "<html/>"
        every { bodyRenderer.renderText(any()) } returns "내용"
        every { unsubscribe.tokenFor(any()) } returns "tok"
    }

    describe("start") {
        it("시작 시 대상을 수신자 행으로 고정하고 [AUDIT] 로그를 남긴다") {
            val saved = slot<List<MailCampaignRecipient>>()
            every { recipientRepository.saveAll(capture(saved)) } answers { firstArg() }
            every { campaignRepository.findById(any()) } returns java.util.Optional.of(campaign())

            sut().start("admin", body)

            saved.captured.map { it.email } shouldBe listOf("a@konkuk.ac.kr", "b@konkuk.ac.kr")
            saved.captured.map { it.workspaceName } shouldBe listOf("달빛", "별빛")
            saved.captured.all { it.status == MailRecipientStatus.PENDING } shouldBe true
            val logged = appender.list.joinToString("\n") { it.formattedMessage }
            logged shouldContain "[AUDIT] action=START_MAIL_CAMPAIGN"
            logged shouldContain "targetCount=2"
            logged shouldNotContain "a@konkuk"
            verify(exactly = 1) { sender.sendDueAsync() }
        }

        it("확인한 곳 수와 지금 대상 수가 다르면 만들지 않는다") {
            shouldThrow<CustomException> { sut().start("admin", body.copy(expectedCount = 3)) }
                .errorCode shouldBe ErrorCode.MAIL_AUDIENCE_CHANGED

            verify(exactly = 0) { campaignRepository.save(any<MailCampaign>()) }
            verify(exactly = 0) { sender.sendDueAsync() }
        }

        it("확인한 곳 수가 없으면 만들지 않는다") {
            shouldThrow<CustomException> { sut().start("admin", body.copy(expectedCount = null)) }
                .errorCode shouldBe ErrorCode.MAIL_AUDIENCE_CHANGED

            verify(exactly = 0) { campaignRepository.save(any<MailCampaign>()) }
        }

        it("같은 요청 번호로 다시 시작하면 새로 만들지 않고 기존 발송을 돌려준다") {
            every { campaignRepository.findByRequestId("req-1") } returns campaign()

            sut().start("admin", body).subject shouldBe "s"

            verify(exactly = 0) { campaignRepository.save(any<MailCampaign>()) }
            verify(exactly = 0) { sender.sendDueAsync() }
        }

        it("같은 요청 번호가 동시에 들어와 unique에 걸리면 기존 발송을 돌려준다") {
            every { campaignRepository.findByRequestId("req-1") } returns null andThen campaign()
            every { campaignRepository.save(any<MailCampaign>()) } throws org.springframework.dao.DataIntegrityViolationException("dup")

            sut().start("admin", body).subject shouldBe "s"

            verify(exactly = 0) { sender.sendDueAsync() }
        }

        it("요청 번호를 발송에 저장한다") {
            val saved = slot<MailCampaign>()
            every { campaignRepository.save(capture(saved)) } answers { firstArg() }

            sut().start("admin", body)

            saved.captured.requestId shouldBe "req-1"
        }

        it("대상이 0곳이면 만들지 않는다") {
            every { audience.resolve(filter) } returns emptyList()

            shouldThrow<CustomException> { sut().start("admin", body) }.errorCode shouldBe ErrorCode.MAIL_RECIPIENT_UNAVAILABLE

            verify(exactly = 0) { campaignRepository.save(any<MailCampaign>()) }
            verify(exactly = 0) { sender.sendDueAsync() }
        }

        it("템플릿이 잘못되면 만들지 않는다") {
            shouldThrow<CustomException> { sut().start("admin", body.copy(subject = "{모름}")) }
                .errorCode shouldBe ErrorCode.MAIL_TEMPLATE_INVALID

            verify(exactly = 0) { campaignRepository.save(any<MailCampaign>()) }
        }
    }

    describe("start의 내용") {
        it("정리한 HTML과 고른 기간 값을 저장한다") {
            val campaignSaved = slot<MailCampaign>()
            val recipients = slot<List<MailCampaignRecipient>>()
            every { campaignRepository.save(capture(campaignSaved)) } answers { firstArg() }
            every { recipientRepository.saveAll(capture(recipients)) } answers { firstArg() }
            every { audience.resolve(filter) } returns listOf(MailAudienceRow(1, "달빛", "건국대학교", "김사장", "a@konkuk.ac.kr", orderCount = 812))

            sut().start("admin", body.copy(content = "<p>안녕<script>x</script></p>", expectedCount = 1))

            campaignSaved.captured.bodyTemplate shouldBe "<p>안녕</p>"
            mapper.readValue(recipients.captured.single().statsJson, MailAudienceStats::class.java).orderCount shouldBe 812
        }

        it("내용이 비어 있으면 만들지 않는다") {
            shouldThrow<CustomException> { sut().start("admin", body.copy(content = "<p> </p>")) }
                .errorCode shouldBe ErrorCode.MAIL_TEMPLATE_INVALID
        }
    }

    describe("previewMail") {
        val previewBody = MailCampaignPreviewRequestBody(filter, "{주점 이름} 안내", "<p>{사장 이름}님</p>")

        it("첫 주점의 값으로 제목과 받는 사람을 돌려준다") {
            val preview = sut().previewMail(previewBody)

            preview.subject shouldBe "달빛 안내"
            preview.workspaceName shouldBe "달빛"
            preview.ownerEmail shouldBe "a@konkuk.ac.kr"
            preview.html shouldBe "<html/>"
            verify { bodyRenderer.render("<p>김사장님</p>", any()) }
        }

        it("받을 주점이 없으면 값 자리를 그대로 보여준다") {
            every { audience.resolve(filter) } returns emptyList()

            val preview = sut().previewMail(previewBody)

            preview.subject shouldBe "{주점 이름} 안내"
            preview.workspaceName shouldBe null
            verify { bodyRenderer.render("<p>{사장 이름}님</p>", any()) }
        }

        it("모르는 값이 있으면 MAIL_TEMPLATE_INVALID") {
            shouldThrow<CustomException> { sut().previewMail(previewBody.copy(content = "<p>{모름}</p>")) }
                .errorCode shouldBe ErrorCode.MAIL_TEMPLATE_INVALID
        }

        it("입력 전이어서 내용이 비어 있어도 된다") {
            sut().previewMail(previewBody.copy(subject = "", content = "")).html shouldBe "<html/>"
        }
    }

    describe("stop") {
        it("멈추면 STOPPED로 바꾸고 [AUDIT] 로그를 남긴다") {
            val target = campaign()
            every { campaignRepository.findByIdOrNull(1) } returns target
            every { recipientRepository.findAllByCampaignOrderByIdAsc(any(), any()) } returns org.springframework.data.domain.PageImpl(emptyList())

            sut().stop("admin", 1, 0, 20)

            target.status shouldBe MailCampaignStatus.STOPPED
            appender.list.joinToString("\n") { it.formattedMessage } shouldContain "[AUDIT] action=STOP_MAIL_CAMPAIGN"
        }

        it("없는 발송이면 MAIL_CAMPAIGN_NOT_FOUND") {
            every { campaignRepository.findByIdOrNull(404) } returns null

            shouldThrow<CustomException> { sut().stop("admin", 404, 0, 20) }.errorCode shouldBe ErrorCode.MAIL_CAMPAIGN_NOT_FOUND
        }
    }

    describe("retryFailed") {
        it("attempt를 올리고 PENDING으로 돌린다") {
            val target = campaign(MailCampaignStatus.DONE)
            val claimed = java.time.LocalDateTime.of(2026, 11, 2, 12, 0)
            val failed = MailCampaignRecipient(target, null, "달빛", "a@konkuk.ac.kr", "김사장", status = MailRecipientStatus.FAILED, failureReason = "x", attempt = 1, claimedAt = claimed)
            every { campaignRepository.findByIdOrNull(1) } returns target
            every { recipientRepository.findAllByCampaignAndStatus(target, MailRecipientStatus.FAILED) } returns listOf(failed)
            every { recipientRepository.findAllByCampaignOrderByIdAsc(any(), any()) } returns org.springframework.data.domain.PageImpl(listOf(failed))

            sut().retryFailed(1, 0, 20)

            failed.status shouldBe MailRecipientStatus.PENDING
            failed.attempt shouldBe 2
            failed.failureReason shouldBe null
            failed.claimedAt shouldBe claimed
            target.status shouldBe MailCampaignStatus.SENDING
            verify(exactly = 1) { sender.sendDueAsync() }
        }

        it("멈춘 발송은 다시 시작하지 않는다") {
            val target = campaign(MailCampaignStatus.STOPPED)
            every { campaignRepository.findByIdOrNull(1) } returns target
            every { recipientRepository.findAllByCampaignOrderByIdAsc(any(), any()) } returns org.springframework.data.domain.PageImpl(emptyList())

            sut().retryFailed(1, 0, 20)

            target.status shouldBe MailCampaignStatus.STOPPED
            verify(exactly = 0) { sender.sendDueAsync() }
        }
    }

    describe("test") {
        it("본인 이메일이 없으면 MAIL_RECIPIENT_UNAVAILABLE") {
            every { userService.getUser("admin") } returns User(
                loginId = "a", loginPassword = "p", name = "관리자", email = null, role = UserRole.SUPER_ADMIN, members = mutableListOf(),
            )

            shouldThrow<CustomException> { sut().test("admin", body) }.errorCode shouldBe ErrorCode.MAIL_RECIPIENT_UNAVAILABLE
            verify { client wasNot Called }
        }

        it("대상이 없으면 MAIL_RECIPIENT_UNAVAILABLE") {
            every { audience.resolve(filter) } returns emptyList()

            shouldThrow<CustomException> { sut().test("admin", body) }.errorCode shouldBe ErrorCode.MAIL_RECIPIENT_UNAVAILABLE
        }

        it("본인 이메일로 첫 주점 값을 넣어 [테스트]를 제목 앞에 붙여 보낸다") {
            val subject = slot<String>()
            every { client.sendToOwnAddress("test@test.com", capture(subject), any(), any(), any(), any()) } returns "e"

            sut().test("admin", body)

            subject.captured shouldStartWith "[테스트] 달빛 안내"
            verify(exactly = 0) { campaignRepository.save(any<MailCampaign>()) }
        }

        it("본인 주소라서 허용 도메인 검사 없이 보낸다") {
            every { client.sendToOwnAddress(any(), any(), any(), any(), any(), any()) } returns "e"

            sut().test("admin", body)

            verify(exactly = 1) { client.sendToOwnAddress("test@test.com", any(), any(), any(), any(), any()) }
            verify(exactly = 0) { client.sendNew(any(), any(), any(), any(), any(), any()) }
        }
    }
})
