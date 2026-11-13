package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaign
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaignRecipient
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailCampaignStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRecipientRepository
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRepository
import com.kioschool.kioschoolapi.domain.mailbox.service.MailBodyRenderer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailUnsubscribeService
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.resend.service.ResendDailyLimitException
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import com.kioschool.kioschoolapi.global.resend.service.ResendSendException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.*
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.time.LocalDateTime

class MailCampaignSenderTest : DescribeSpec({
    val campaignRepository = mockk<MailCampaignRepository>()
    val recipientRepository = mockk<MailCampaignRecipientRepository>()
    val unsubscribeService = mockk<MailUnsubscribeService>()
    val client = mockk<ResendInboxClient>()
    val sync = mockk<MailSyncService>(relaxed = true)
    val engine = SpringTemplateEngine().apply {
        setTemplateResolver(
            ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
                suffix = ".html"
                characterEncoding = "UTF-8"
                templateMode = TemplateMode.HTML
            }
        )
    }
    val mapper = Jackson2ObjectMapperBuilder.json().build<com.fasterxml.jackson.databind.ObjectMapper>()
    val queue = MailCampaignQueue(campaignRepository, recipientRepository, unsubscribeService, mapper, 365)
    val sanitizer = MailHtmlSanitizer()
    val composer = MailCampaignComposer(MailTemplateRenderer(sanitizer), MailBodyRenderer(engine, sanitizer), "https://kio-school.com", "https://api.kio-school.com")
    lateinit var sut: MailCampaignSender

    val noon = LocalDateTime.of(2026, 11, 2, 12, 0)
    val recipients = mutableListOf<MailCampaignRecipient>()
    val campaign = MailCampaign(
        subject = "{주점 이름} 안내",
        bodyTemplate = "<p>{사장 이름}님 안녕하세요</p>",
        filterJson = "{}",
        createdBy = null,
        purgeAt = noon.plusDays(1),
    )

    fun setId(entity: BaseEntity, id: Long) {
        val f = BaseEntity::class.java.getDeclaredField("id")
        f.isAccessible = true
        f.set(entity, id)
    }

    fun workspace(id: Long, name: String = "주점$id", email: String? = "o$id@kio-school.com") =
        Workspace(
            name = name,
            owner = User(loginId = "o$id", loginPassword = "p", name = "사장$id", email = email, role = UserRole.ADMIN, members = mutableListOf()),
            workspaceSetting = WorkspaceSetting(),
        ).also { setId(it, id) }

    fun addRecipient(id: Long, ws: Workspace? = workspace(id), attempt: Int = 0, email: String = "old$id@kio-school.com") =
        MailCampaignRecipient(campaign, ws, ws?.name ?: "지운 주점", email, "사장$id", attempt = attempt).also {
            setId(it, id)
            recipients += it
        }

    beforeTest {
        clearAllMocks()
        sut = MailCampaignSender(queue, composer, client, sync, 70, 0)
        recipients.clear()
        campaign.status = MailCampaignStatus.SENDING
        campaign.purgeAt = noon.plusDays(1)
        setId(campaign, 1)
        campaign.bodyTemplate = "<p>{사장 이름}님 안녕하세요</p>"
        campaign.signatureJson = null
        every { recipientRepository.findNextPendingForUpdate() } answers {
            recipients.firstOrNull { it.status == MailRecipientStatus.PENDING && it.campaign.status == MailCampaignStatus.SENDING }
        }
        every { recipientRepository.findById(any()) } answers { java.util.Optional.of(recipients.first { it.id == firstArg<Long>() }) }
        every { recipientRepository.save(any<MailCampaignRecipient>()) } answers { firstArg() }
        every { recipientRepository.countAttempts(any(), any(), any()) } returns 0
        every { recipientRepository.findAllByStatusAndClaimedAtBefore(any(), any()) } returns emptyList()
        every { recipientRepository.countUnfinished(campaign) } answers {
            recipients.count { it.status == MailRecipientStatus.PENDING || it.status == MailRecipientStatus.SENDING }.toLong()
        }
        every { campaignRepository.findAllByStatus(MailCampaignStatus.SENDING) } answers {
            if (campaign.status == MailCampaignStatus.SENDING) listOf(campaign) else emptyList()
        }
        every { campaignRepository.save(any<MailCampaign>()) } answers { firstArg() }
        every { unsubscribeService.tokenFor(any()) } returns "tok"
        every { client.sendNew(any(), any(), any(), any(), any(), any()) } returns "email-id"
    }

    describe("sendDue") {
        it("발송을 만들 때 저장한 서명을 서명 자리에 넣는다") {
            campaign.bodyTemplate = """<p>{사장 이름}님 안녕하세요</p><div data-mail-signature=""></div>"""
            campaign.signatureJson = mapper.writeValueAsString(MailSignatureBlock("<div>팀 서명</div>", "--\n팀 서명"))
            addRecipient(1)
            val html = slot<String>()
            val text = slot<String>()
            every { client.sendNew(any(), any(), capture(html), capture(text), any(), any()) } returns "email-id"

            sut.sendDue(noon)

            html.captured shouldContain "<div>팀 서명</div>"
            html.captured shouldNotContain "data-mail-signature"
            text.captured shouldContain "팀 서명"
        }

        it("하루 한도 70곳까지만 보내고 나머지는 PENDING으로 둔다") {
            (1L..75L).forEach { addRecipient(it) }

            sut.sendDue(noon)

            recipients.count { it.status == MailRecipientStatus.SENT } shouldBe 70
            recipients.count { it.status == MailRecipientStatus.PENDING } shouldBe 5
            campaign.status shouldBe MailCampaignStatus.SENDING
        }

        it("오늘 이미 60곳을 시도했으면 10곳만 보낸다") {
            every { recipientRepository.countAttempts(any(), any(), any()) } returns 60
            (1L..15L).forEach { addRecipient(it) }

            sut.sendDue(noon)

            recipients.count { it.status == MailRecipientStatus.SENT } shouldBe 10
        }

        it("오늘 시도한 수를 KST 하루 기준으로 센다. 개별 메일은 이 표에 없어 세지 않는다") {
            sut.todayAttempts(noon)

            verify {
                recipientRepository.countAttempts(
                    LocalDateTime.of(2026, 11, 2, 0, 0),
                    LocalDateTime.of(2026, 11, 3, 0, 0),
                    setOf(MailRecipientStatus.SENT, MailRecipientStatus.FAILED, MailRecipientStatus.SENDING, MailRecipientStatus.PENDING),
                )
            }
        }

        listOf(
            LocalDateTime.of(2026, 11, 2, 9, 59) to 0,
            LocalDateTime.of(2026, 11, 2, 20, 0) to 0,
            LocalDateTime.of(2026, 11, 2, 10, 0) to 1,
            LocalDateTime.of(2026, 11, 2, 19, 59) to 1,
        ).forEach { (at, expected) ->
            it("${at.toLocalTime()}에는 ${if (expected == 0) "보내지 않는다" else "보낸다"}") {
                addRecipient(1)

                sut.sendDue(at)

                verify(exactly = expected) { client.sendNew(any(), any(), any(), any(), any(), any()) }
            }
        }

        it("STOPPED 캠페인은 보내지 않는다") {
            campaign.status = MailCampaignStatus.STOPPED
            addRecipient(1)

            sut.sendDue(noon)

            verify { client wasNot Called }
            recipients.single().status shouldBe MailRecipientStatus.PENDING
        }

        it("Idempotency-Key는 mail-campaign-{수신자ID}-{attempt}이다") {
            addRecipient(7, attempt = 2)

            sut.sendDue(noon)

            verify { client.sendNew(any(), any(), any(), any(), any(), "mail-campaign-7-2") }
        }

        it("ResendDailyLimitException이면 그 수신자를 PENDING으로 돌리고 오늘 발송을 멈춘다") {
            addRecipient(1)
            addRecipient(2)
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } throws ResendDailyLimitException()

            sut.sendDue(noon)

            verify(exactly = 1) { client.sendNew(any(), any(), any(), any(), any(), any()) }
            recipients.all { it.status == MailRecipientStatus.PENDING } shouldBe true
            recipients[0].claimedAt.shouldBeNull()
        }

        it("한도 초과가 나면 같은 날 다시 불러도 보내지 않고 다음 날 다시 보낸다") {
            val tomorrow = noon.plusDays(1)
            addRecipient(1)
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } throws ResendDailyLimitException() andThen "email-id"

            sut.sendDue(noon)
            sut.sendDue(noon.plusMinutes(10))
            verify(exactly = 1) { client.sendNew(any(), any(), any(), any(), any(), any()) }

            sut.sendDue(tomorrow)
            verify(exactly = 2) { client.sendNew(any(), any(), any(), any(), any(), any()) }
            recipients.single().status shouldBe MailRecipientStatus.SENT
        }

        it("ResendSendException이면 FAILED와 사유를 남기고 다음 곳으로 넘어간다") {
            addRecipient(1)
            addRecipient(2)
            every { client.sendNew(any(), any(), any(), any(), any(), "mail-campaign-1-0") } throws ResendSendException("Resend 응답 500")

            sut.sendDue(noon)

            recipients[0].status shouldBe MailRecipientStatus.FAILED
            recipients[0].failureReason.shouldNotBeNull()
            recipients[1].status shouldBe MailRecipientStatus.SENT
        }

        it("보내기 직전 사장이 탈퇴했으면 EXCLUDED로 두고 보내지 않는다") {
            val ws = workspace(1, email = "withdrawn-1@konkuk.ac.kr").apply { owner.withdrawnAt = noon }
            addRecipient(1, ws)

            sut.sendDue(noon)

            recipients.single().status shouldBe MailRecipientStatus.EXCLUDED
            verify { client wasNot Called }
        }

        it("보내기 직전 수신 거부했으면 EXCLUDED로 두고 보내지 않는다") {
            val ws = workspace(1).apply { owner.mailUnsubscribedAt = noon }
            addRecipient(1, ws)

            sut.sendDue(noon)

            recipients.single().status shouldBe MailRecipientStatus.EXCLUDED
            verify { client wasNot Called }
        }

        it("주점이 지워졌거나 사장 이메일이 없으면 EXCLUDED로 두고 한도에 세지 않는다") {
            addRecipient(1, ws = null)
            addRecipient(2, workspace(2, email = null))
            addRecipient(3)

            sut.sendDue(noon)

            recipients[0].status shouldBe MailRecipientStatus.EXCLUDED
            recipients[1].status shouldBe MailRecipientStatus.EXCLUDED
            recipients[2].status shouldBe MailRecipientStatus.SENT
        }

        it("보내기 직전 사장 이메일이 바뀌었으면 새 주소로 보낸다") {
            addRecipient(1, workspace(1, email = "new@kio-school.com"), email = "old@kio-school.com")

            sut.sendDue(noon)

            verify { client.sendNew("new@kio-school.com", any(), any(), any(), any(), any()) }
            recipients.single().email shouldBe "new@kio-school.com"
        }

        it("주점 이름의 <b>가 메일 HTML에서 이스케이프된다") {
            campaign.bodyTemplate = "<p>{주점 이름} 안내</p>"
            val html = slot<String>()
            every { client.sendNew(any(), any(), capture(html), any(), any(), any()) } returns "email-id"
            addRecipient(1, workspace(1, name = "달빛<b>포차"))

            sut.sendDue(noon)

            html.captured shouldContain "달빛&lt;b&gt;포차"
            html.captured shouldNotContain "달빛<b>포차"
            campaign.bodyTemplate = "<p>{사장 이름}님 안녕하세요</p>"
        }

        it("List-Unsubscribe와 List-Unsubscribe-Post 헤더, 수신 거부 링크를 넣는다") {
            val headers = slot<Map<String, String>>()
            val html = slot<String>()
            val text = slot<String>()
            every { client.sendNew(any(), any(), capture(html), capture(text), capture(headers), any()) } returns "email-id"
            addRecipient(1)

            sut.sendDue(noon)

            headers.captured["List-Unsubscribe"] shouldBe "<https://api.kio-school.com/mail/unsubscribe?token=tok>"
            headers.captured["List-Unsubscribe-Post"] shouldBe "List-Unsubscribe=One-Click"
            html.captured shouldContain "https://kio-school.com/mail/unsubscribe?token=tok"
            text.captured shouldContain "https://kio-school.com/mail/unsubscribe?token=tok"
        }

        it("제목과 내용을 주점마다 치환하고 발송을 기록한다") {
            val subject = slot<String>()
            every { client.sendNew(any(), capture(subject), any(), any(), any(), any()) } returns "email-id"
            val ws = workspace(1)
            addRecipient(1, ws)

            sut.sendDue(noon)

            subject.captured shouldBe "주점1 안내"
            verify { sync.recordOutbound("email-id", null, ws, 1, null, "주점1 안내", any(), any(), "o1@kio-school.com") }
            recipients.single().resendEmailId shouldBe "email-id"
            recipients.single().sentAt shouldBe noon
        }

        it("시작할 때 고정한 고른 기간 값을 메일에 넣는다") {
            campaign.bodyTemplate = "<p>{주문 수}건 {매출} {인기 메뉴}</p>"
            val html = slot<String>()
            val text = slot<String>()
            every { client.sendNew(any(), any(), capture(html), capture(text), any(), any()) } returns "email-id"
            addRecipient(1).statsJson = mapper.writeValueAsString(MailAudienceStats(orderCount = 1812, revenue = 2000, topMenu = "김치전"))

            sut.sendDue(noon)

            html.captured shouldContain "1,812건 2,000원 김치전"
            text.captured shouldContain "1,812건 2,000원 김치전"
            campaign.bodyTemplate = "<p>{사장 이름}님 안녕하세요</p>"
        }

        it("10분 넘게 SENDING인 행은 PENDING으로 돌려 같은 키로 다시 보낸다") {
            val stale = addRecipient(1, attempt = 1).apply {
                status = MailRecipientStatus.SENDING
                claimedAt = noon.minusMinutes(11)
            }
            every { recipientRepository.findAllByStatusAndClaimedAtBefore(MailRecipientStatus.SENDING, noon.minusMinutes(10)) } returns listOf(stale)

            sut.sendDue(noon)

            verify { client.sendNew(any(), any(), any(), any(), any(), "mail-campaign-1-1") }
            stale.status shouldBe MailRecipientStatus.SENT
        }

        it("모두 끝나면 DONE이 되고 purgeAt을 365일 뒤로 맞춘다") {
            addRecipient(1)

            sut.sendDue(noon)

            campaign.status shouldBe MailCampaignStatus.DONE
            campaign.purgeAt shouldBe noon.plusDays(365)
        }

        it("발송 기록이 실패해도 수신자는 SENT로 남는다") {
            every { sync.recordOutbound(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("db")
            addRecipient(1)

            sut.sendDue(noon)

            recipients.single().status shouldBe MailRecipientStatus.SENT
        }
    }

    describe("MailCampaignQueue.markBounced") {
        it("수신자 메일이면 FAILED로 바꾸고 사유를 남긴다") {
            val recipient = addRecipient(1).apply { status = MailRecipientStatus.SENT }
            every { recipientRepository.findByResendEmailId("e1") } returns recipient

            queue.markBounced("e1")

            recipient.status shouldBe MailRecipientStatus.FAILED
            recipient.failureReason shouldBe "메일 주소를 찾을 수 없어요"
        }

        it("모르는 메일이면 아무것도 하지 않는다") {
            every { recipientRepository.findByResendEmailId("e1") } returns null

            queue.markBounced("e1")

            verify(exactly = 0) { recipientRepository.save(any<MailCampaignRecipient>()) }
        }
    }
})
