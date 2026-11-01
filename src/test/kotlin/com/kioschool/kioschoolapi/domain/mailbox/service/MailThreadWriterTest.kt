package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailMessageRepository
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailThreadRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.resend.dto.ResendAttachment
import com.kioschool.kioschoolapi.global.resend.dto.ResendThreadEmail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.time.OffsetDateTime

class MailThreadWriterTest : DescribeSpec({
    val threadRepository = mockk<MailThreadRepository>()
    val messageRepository = mockk<MailMessageRepository>()
    val workspaceRepository = mockk<WorkspaceRepository>()
    val sut = MailThreadWriter(threadRepository, messageRepository, workspaceRepository, 365)

    val threads = mutableMapOf<String, MailThread>()
    val messages = mutableMapOf<String, MailMessage>()

    beforeTest {
        clearAllMocks()
        threads.clear()
        messages.clear()
        every { threadRepository.findByResendThreadId(any()) } answers { threads[firstArg()] }
        every { threadRepository.save(any<MailThread>()) } answers {
            firstArg<MailThread>().also { threads[it.resendThreadId] = it }
        }
        every { messageRepository.findByResendEmailId(any()) } answers { messages[firstArg()] }
        every { messageRepository.save(any<MailMessage>()) } answers {
            firstArg<MailMessage>().also { messages[it.resendEmailId] = it }
        }
        every { workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdDesc(any()) } returns null
    }

    fun email(
        id: String = "m1",
        direction: String = "inbound",
        from: String = "\"사장\" <Owner@Test.com>",
        to: List<String> = listOf("contact@kio-school.com"),
        subject: String? = "문의",
        html: String? = "<p>본문</p>",
        text: String? = "본문",
        attachments: List<ResendAttachment> = emptyList(),
        at: String = "2026-11-01T03:00:00Z",
    ) = ResendThreadEmail(id, direction, from, to, subject, html, text, attachments, OffsetDateTime.parse(at))

    describe("upsert") {
        it("같은 이메일을 두 번 넣어도 한 행이고 newInboundCount는 처음만 센다") {
            val emails = listOf(email())

            sut.upsert("t1", emails).newInboundCount shouldBe 1
            sut.upsert("t1", emails).newInboundCount shouldBe 0

            messages.size shouldBe 1
            threads.size shouldBe 1
        }

        it("메일 시각을 KST로 저장하고 보낸 사람 이름과 소문자 주소를 나눈다") {
            sut.upsert("t1", listOf(email()))

            val message = messages.getValue("m1")
            message.sentAt shouldBe LocalDateTime.of(2026, 11, 1, 12, 0)
            message.fromName shouldBe "사장"
            message.fromEmail shouldBe "owner@test.com"
            message.toEmail shouldBe "contact@kio-school.com"
            threads.getValue("t1").counterpartEmail shouldBe "owner@test.com"
            threads.getValue("t1").counterpartName shouldBe "사장"
        }

        it("새 받은 메일이면 resolvedAt을 비우고 lastDirection을 INBOUND로 바꾼다") {
            val thread = SampleEntity.newMailThread(
                resendThreadId = "t1",
                lastDirection = MailDirection.OUTBOUND,
                resolvedAt = LocalDateTime.of(2026, 10, 1, 0, 0),
            )
            threads["t1"] = thread

            sut.upsert("t1", listOf(email(at = "2026-11-02T00:00:00Z")))

            thread.resolvedAt.shouldBeNull()
            thread.lastDirection shouldBe MailDirection.INBOUND
            thread.lastMessageAt shouldBe LocalDateTime.of(2026, 11, 2, 9, 0)
        }

        it("이미 저장한 메일만 다시 오면 완료 상태를 건드리지 않는다") {
            sut.upsert("t1", listOf(email()))
            val resolved = LocalDateTime.of(2026, 11, 3, 0, 0)
            threads.getValue("t1").resolvedAt = resolved

            sut.upsert("t1", listOf(email()))

            threads.getValue("t1").resolvedAt shouldBe resolved
        }

        it("순서가 뒤바뀌어 와도 가장 최근 메일로 상태를 맞춘다") {
            sut.upsert(
                "t1",
                listOf(
                    email(id = "new", direction = "outbound", from = "키오스쿨 <contact@kio-school.com>", to = listOf("owner@test.com"), at = "2026-11-02T00:00:00Z"),
                    email(id = "old", at = "2026-11-01T00:00:00Z"),
                )
            )

            threads.getValue("t1").lastDirection shouldBe MailDirection.OUTBOUND
            threads.getValue("t1").lastMessageAt shouldBe LocalDateTime.of(2026, 11, 2, 9, 0)
        }

        it("받은 메일이 없으면 보낸 메일의 첫 받는 사람을 상대방으로 둔다") {
            sut.upsert(
                "t1",
                listOf(email(direction = "outbound", from = "키오스쿨 <contact@kio-school.com>", to = listOf("\"사장\" <Owner@Test.com>", "other@test.com")))
            )

            threads.getValue("t1").counterpartEmail shouldBe "owner@test.com"
            threads.getValue("t1").counterpartName shouldBe "사장"
            threads.getValue("t1").hasInbound shouldBe false
        }

        it("주점이 비어 있으면 보낸 사람이 사장인 주점을 찾는다") {
            val workspace = SampleEntity.workspace
            every { workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdDesc("owner@test.com") } returns workspace

            sut.upsert("t1", listOf(email()))

            threads.getValue("t1").workspace shouldBe workspace
        }

        it("workspaceHint가 있는 메일의 주점이 이메일 조회보다 우선한다") {
            val hinted = SampleEntity.workspace
            messages["m1"] = MailMessage(resendEmailId = "m1", workspaceHint = hinted, direction = MailDirection.OUTBOUND)

            sut.upsert("t1", listOf(email(id = "m1", direction = "outbound", from = "키오스쿨 <contact@kio-school.com>", to = listOf("owner@test.com"))))

            threads.getValue("t1").workspace shouldBe hinted
            verify(exactly = 0) { workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdDesc(any()) }
        }

        it("이미 연결한 주점은 바꾸지 않는다") {
            val linked = SampleEntity.workspace
            threads["t1"] = SampleEntity.newMailThread(resendThreadId = "t1", workspace = linked)

            sut.upsert("t1", listOf(email()))

            threads.getValue("t1").workspace shouldBe linked
            verify(exactly = 0) { workspaceRepository.findFirstByOwnerEmailIgnoreCaseOrderByIdDesc(any()) }
        }

        it("주점을 못 찾으면 null로 둔다") {
            sut.upsert("t1", listOf(email()))

            threads.getValue("t1").workspace.shouldBeNull()
        }

        it("제목 null, 2000자 제목, html·text 모두 null이어도 저장한다") {
            val long = "가".repeat(2000)

            sut.upsert(
                "t1",
                listOf(
                    email(id = "a", subject = null, html = null, text = null, at = "2026-11-01T00:00:00Z"),
                    email(id = "b", subject = long, html = null, text = null, at = "2026-11-01T01:00:00Z"),
                )
            )

            messages.getValue("a").subject.shouldBeNull()
            messages.getValue("a").textBody.shouldBeNull()
            messages.getValue("b").subject shouldBe long
            threads.getValue("t1").subject shouldBe long
        }

        it("본문의 NUL 문자를 지우고 저장한다") {
            sut.upsert("t1", listOf(email(subject = "제\u0000목", text = "본\u0000문", html = "<p>\u0000</p>")))

            messages.getValue("m1").subject shouldBe "제목"
            messages.getValue("m1").textBody shouldBe "본문"
            messages.getValue("m1").htmlBody shouldBe "<p></p>"
        }

        it("첨부를 한 번만 저장한다") {
            val attachments = listOf(ResendAttachment("a1", "통장.pdf", 10), ResendAttachment("a2", null, null))

            sut.upsert("t1", listOf(email(attachments = attachments)))
            sut.upsert("t1", listOf(email(attachments = attachments)))

            messages.getValue("m1").attachments.map { it.resendAttachmentId } shouldBe listOf("a1", "a2")
            messages.getValue("m1").attachments[1].filename.shouldBeNull()
        }

        it("hasInbound와 fromCampaign을 채우고 purgeAt을 갱신한다") {
            messages["c"] = MailMessage(resendEmailId = "c", campaignRecipientId = 7, direction = MailDirection.OUTBOUND)

            sut.upsert(
                "t1",
                listOf(
                    email(id = "c", direction = "outbound", from = "키오스쿨 <contact@kio-school.com>", to = listOf("owner@test.com"), at = "2026-11-01T00:00:00Z"),
                    email(id = "r", at = "2026-11-02T00:00:00Z"),
                )
            )

            val thread = threads.getValue("t1")
            thread.hasInbound shouldBe true
            thread.fromCampaign shouldBe true
            thread.purgeAt shouldBe LocalDateTime.of(2026, 11, 2, 9, 0).plusDays(365)
        }

        it("아웃바운드로 먼저 기록된 행에 스레드와 본문을 채운다") {
            val recorded = MailMessage(resendEmailId = "m1", direction = MailDirection.OUTBOUND, sentBy = SampleEntity.user)
            messages["m1"] = recorded

            val result = sut.upsert(
                "t1",
                listOf(email(id = "m1", direction = "outbound", from = "키오스쿨 <contact@kio-school.com>", to = listOf("owner@test.com")))
            )

            result.newInboundCount shouldBe 0
            recorded.thread.shouldNotBeNull().resendThreadId shouldBe "t1"
            recorded.textBody shouldBe "본문"
            recorded.sentBy shouldBe SampleEntity.user
        }

        it("메일이 없으면 아무것도 저장하지 않는다") {
            sut.upsert("t1", emptyList()).newInboundCount shouldBe 0

            threads.size shouldBe 0
        }
    }

    describe("recordOutbound") {
        fun record(thread: MailThread? = null, workspace: com.kioschool.kioschoolapi.domain.workspace.entity.Workspace? = null, campaignRecipientId: Long? = null) =
            sut.recordOutbound("m1", SampleEntity.user, workspace, campaignRecipientId, thread, "제목", "내용", "<p>내용</p>", "owner@test.com")

        it("웹훅보다 먼저면 내용을 담은 행을 만든다") {
            record(workspace = SampleEntity.workspace)

            val message = messages.getValue("m1")
            message.direction shouldBe MailDirection.OUTBOUND
            message.subject shouldBe "제목"
            message.textBody shouldBe "내용"
            message.toEmail shouldBe "owner@test.com"
            message.sentBy shouldBe SampleEntity.user
            message.workspaceHint shouldBe SampleEntity.workspace
            message.sentAt.shouldNotBeNull()
        }

        it("이미 동기화된 행이면 sentBy와 workspaceHint만 채우고 스레드 주점이 비었으면 채운다") {
            val thread = SampleEntity.newMailThread(resendThreadId = "t1")
            val synced = MailMessage(resendEmailId = "m1", thread = thread, direction = MailDirection.OUTBOUND, textBody = "웹훅 본문")
            messages["m1"] = synced

            record(workspace = SampleEntity.workspace)

            synced.textBody shouldBe "웹훅 본문"
            synced.sentBy shouldBe SampleEntity.user
            synced.workspaceHint shouldBe SampleEntity.workspace
            thread.workspace shouldBe SampleEntity.workspace
        }

        it("대량 발송 메일이 이미 동기화됐으면 스레드에 fromCampaign을 표시한다") {
            val thread = SampleEntity.newMailThread(resendThreadId = "t1")
            messages["m1"] = MailMessage(resendEmailId = "m1", thread = thread, direction = MailDirection.OUTBOUND)

            record(campaignRecipientId = 3)

            messages.getValue("m1").campaignRecipientId shouldBe 3
            thread.fromCampaign shouldBe true
        }

        it("답장이면 대화를 답장 대기로 되돌린다") {
            val thread = SampleEntity.newMailThread(
                resendThreadId = "t1",
                lastDirection = MailDirection.INBOUND,
                resolvedAt = LocalDateTime.of(2026, 10, 1, 0, 0),
            )
            every { threadRepository.findByIdOrNull(any()) } returns thread

            record(thread = thread)

            thread.lastDirection shouldBe MailDirection.OUTBOUND
            thread.resolvedAt.shouldBeNull()
            messages.getValue("m1").thread shouldBe thread
        }
    }
})
