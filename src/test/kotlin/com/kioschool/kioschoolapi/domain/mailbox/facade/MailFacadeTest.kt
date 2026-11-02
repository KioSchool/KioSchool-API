package com.kioschool.kioschoolapi.domain.mailbox.facade

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.email.service.SchoolResolver
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.MailPreviewRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.ReplyMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.SendMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailAttachment
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailAttachmentRepository
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailMessageRepository
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailThreadRepository
import com.kioschool.kioschoolapi.domain.mailbox.service.MailBodyRenderer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailPurgeService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailThreadWriter
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.resend.service.MailRecipientGuard
import com.kioschool.kioschoolapi.global.resend.service.ResendAttachmentDownloader
import com.kioschool.kioschoolapi.global.resend.service.ResendDailyLimitException
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import com.kioschool.kioschoolapi.global.resend.service.ResendSendException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.mock.web.MockHttpServletResponse
import java.time.LocalDateTime

class MailFacadeTest : DescribeSpec({
    val threadRepository = mockk<MailThreadRepository>()
    val messageRepository = mockk<MailMessageRepository>()
    val attachmentRepository = mockk<MailAttachmentRepository>()
    val writer = mockk<MailThreadWriter>(relaxed = true)
    val sync = mockk<MailSyncService>(relaxed = true)
    val purge = mockk<MailPurgeService>(relaxed = true)
    val renderer = mockk<MailBodyRenderer>()
    val client = mockk<ResendInboxClient>()
    val downloader = mockk<ResendAttachmentDownloader>()
    val workspaceService = mockk<WorkspaceService>()
    val userService = mockk<UserService>()
    val emailService = mockk<EmailService>()

    fun sut(guard: MailRecipientGuard = MailRecipientGuard("*")) = MailFacade(
        threadRepository, messageRepository, attachmentRepository, writer, sync, purge, renderer, MailHtmlSanitizer(), client,
        downloader, guard, workspaceService, userService, emailService,
    )

    val at = LocalDateTime.of(2026, 11, 1, 12, 0)

    beforeTest {
        clearAllMocks()
        every { renderer.render(any(), any()) } returns "<html/>"
        every { renderer.renderText(any()) } returns "내용"
        every { userService.getUser("admin") } returns SampleEntity.user
        every { emailService.getSchoolResolver() } returns SchoolResolver(emptyMap())
    }

    fun owner(email: String? = "owner@test.com", withdrawnAt: LocalDateTime? = null) =
        SampleEntity.workspace(
            com.kioschool.kioschoolapi.domain.user.entity.User(
                loginId = "o", loginPassword = "p", name = "사장", email = email,
                role = SampleEntity.user.role, members = mutableListOf(),
            ).apply { this.withdrawnAt = withdrawnAt }
        )

    fun message(id: Long, direction: MailDirection, emailId: String = "e$id") =
        MailMessage(resendEmailId = emailId, direction = direction, sentAt = at).also {
            val f = com.kioschool.kioschoolapi.global.common.entity.BaseEntity::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.set(it, id)
        }

    describe("getThreads") {
        it("상태와 주점 조건을 문자열 상태로 넘긴다") {
            val thread = SampleEntity.newMailThread(lastDirection = MailDirection.INBOUND)
            every { threadRepository.search("NEEDS_REPLY", 3L, any()) } returns PageImpl(listOf(thread))

            val page = sut().getThreads(MailThreadStatus.NEEDS_REPLY, 3L, 0, 20)

            page.content.single().status shouldBe MailThreadStatus.NEEDS_REPLY
            page.content.single().isUnread shouldBe true
        }
    }

    describe("getThread") {
        it("읽음으로 표시하고 메일과 학교 이름을 돌려준다") {
            val thread = SampleEntity.newMailThread(workspace = SampleEntity.workspace)
            every { writer.markRead(1) } returns thread
            every { messageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread) } returns listOf(message(1, MailDirection.INBOUND))

            val detail = sut().getThread(1)

            detail.messages.single().id shouldBe 1
            detail.schoolName shouldBe "test.com"
            verify(exactly = 1) { writer.markRead(1) }
        }
    }

    describe("sendNew") {
        val body = SendMailRequestBody(9, "제목", "내용")

        it("받는 주소를 요청이 아니라 주점 사장 이메일에서 가져온다") {
            every { workspaceService.getWorkspace(9) } returns owner()
            every { client.sendNew("owner@test.com", "제목", "<html/>", "내용", emptyMap(), any()) } returns "email"

            sut().sendNew("admin", body)

            verify(exactly = 1) { client.sendNew("owner@test.com", "제목", "<html/>", "내용", emptyMap(), any()) }
        }

        it("내용이 비어 있으면 INVALID_INPUT이고 보내지 않는다") {
            every { workspaceService.getWorkspace(9) } returns owner()

            shouldThrow<CustomException> { sut().sendNew("admin", SendMailRequestBody(9, "제목", "<p> </p>")) }
                .errorCode shouldBe ErrorCode.INVALID_INPUT
            verify { client wasNot Called }
        }

        it("https가 아닌 링크가 있으면 MAIL_TEMPLATE_INVALID이고 보내지 않는다") {
            every { workspaceService.getWorkspace(9) } returns owner()

            shouldThrow<CustomException> {
                sut().sendNew("admin", SendMailRequestBody(9, "제목", """<p><a href="http://x.com">링크</a></p>"""))
            }.errorCode shouldBe ErrorCode.MAIL_TEMPLATE_INVALID
            verify { client wasNot Called }
        }

        it("정리한 HTML로 메일과 글자 본문을 만든다") {
            every { workspaceService.getWorkspace(9) } returns owner()
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } returns "email"

            sut().sendNew("admin", SendMailRequestBody(9, "제목", "<p>안녕<script>x</script></p>"))

            verify { renderer.render("<p>안녕</p>", any()) }
            verify { renderer.renderText("<p>안녕</p>") }
        }

        it("같은 요청 번호로 다시 보내면 같은 Idempotency-Key를 쓴다") {
            every { workspaceService.getWorkspace(9) } returns owner()
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } returns "email"

            sut().sendNew("admin", SendMailRequestBody(9, "제목", "내용", "req-1"))

            verify { client.sendNew(any(), any(), any(), any(), any(), "mail-new-req-1") }
        }

        it("사장 이메일이 null이면 MAIL_RECIPIENT_UNAVAILABLE") {
            every { workspaceService.getWorkspace(9) } returns owner(email = null)

            shouldThrow<CustomException> { sut().sendNew("admin", body) }.errorCode shouldBe ErrorCode.MAIL_RECIPIENT_UNAVAILABLE
            verify { client wasNot Called }
        }

        it("탈퇴한 계정이면 MAIL_RECIPIENT_UNAVAILABLE") {
            every { workspaceService.getWorkspace(9) } returns owner(email = "withdrawn-1@test.com", withdrawnAt = at)

            shouldThrow<CustomException> { sut().sendNew("admin", body) }.errorCode shouldBe ErrorCode.MAIL_RECIPIENT_UNAVAILABLE
            verify { client wasNot Called }
        }

        it("허용 도메인 밖이면 MAIL_RECIPIENT_UNAVAILABLE") {
            every { workspaceService.getWorkspace(9) } returns owner()

            shouldThrow<CustomException> { sut(MailRecipientGuard("kio-school.com")).sendNew("admin", body) }
                .errorCode shouldBe ErrorCode.MAIL_RECIPIENT_UNAVAILABLE
            verify { client wasNot Called }
        }

        it("ResendDailyLimitException이면 MAIL_DAILY_LIMIT_EXCEEDED") {
            every { workspaceService.getWorkspace(9) } returns owner()
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } throws ResendDailyLimitException()

            shouldThrow<CustomException> { sut().sendNew("admin", body) }.errorCode shouldBe ErrorCode.MAIL_DAILY_LIMIT_EXCEEDED
        }

        it("ResendSendException이면 MAIL_SEND_FAILURE") {
            every { workspaceService.getWorkspace(9) } returns owner()
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } throws ResendSendException("x")

            shouldThrow<CustomException> { sut().sendNew("admin", body) }.errorCode shouldBe ErrorCode.MAIL_SEND_FAILURE
        }

        it("성공하면 recordOutbound에 보낸 사람과 주점을 넘긴다") {
            val workspace = owner()
            every { workspaceService.getWorkspace(9) } returns workspace
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } returns "email"

            sut().sendNew("admin", body)

            verify(exactly = 1) {
                sync.recordOutbound("email", SampleEntity.user, workspace, null, null, "제목", "내용", "<html/>", "owner@test.com")
            }
        }

        it("메일이 나간 뒤 기록이 실패해도 오류를 돌려주지 않는다") {
            every { workspaceService.getWorkspace(9) } returns owner()
            every { client.sendNew(any(), any(), any(), any(), any(), any()) } returns "email"
            every { sync.recordOutbound(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("db")

            sut().sendNew("admin", body)
        }
    }

    describe("reply") {
        val thread = SampleEntity.newMailThread(resendThreadId = "t1", lastDirection = MailDirection.INBOUND)

        beforeTest {
            every { threadRepository.findByIdOrNull(1) } returns thread
            every { writer.markRead(1) } returns thread
        }

        it("lastMessageId가 최신 메일이 아니면 MAIL_THREAD_STALE이고 보내지 않는다") {
            every { messageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread) } returns
                listOf(message(1, MailDirection.INBOUND), message(2, MailDirection.INBOUND))

            shouldThrow<CustomException> { sut().reply("admin", 1, ReplyMailRequestBody(1, "내용")) }
                .errorCode shouldBe ErrorCode.MAIL_THREAD_STALE
            verify { client wasNot Called }
        }

        it("대화의 받은 메일 중 마지막에 답장한다") {
            every { messageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread) } returns listOf(
                message(1, MailDirection.INBOUND, "first"),
                message(2, MailDirection.INBOUND, "second"),
                message(3, MailDirection.OUTBOUND, "ours"),
            )
            every { client.reply("t1", "second", "<html/>", "내용", any()) } returns "new"

            sut().reply("admin", 1, ReplyMailRequestBody(3, "내용"))

            verify(exactly = 1) { client.reply("t1", "second", "<html/>", "내용", any()) }
        }

        it("받은 메일이 없으면 마지막 보낸 메일에 이어서 보낸다") {
            every { messageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread) } returns
                listOf(message(1, MailDirection.OUTBOUND, "ours"))
            every { client.reply("t1", "ours", any(), any(), any()) } returns "new"

            sut().reply("admin", 1, ReplyMailRequestBody(1, "내용"))

            verify(exactly = 1) { client.reply("t1", "ours", any(), any(), any()) }
        }

        it("보낸 뒤 대화를 넘겨 recordOutbound로 상태를 바꾼다") {
            every { messageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread) } returns listOf(message(1, MailDirection.INBOUND))
            every { client.reply(any(), any(), any(), any(), any()) } returns "new"

            sut().reply("admin", 1, ReplyMailRequestBody(1, "내용"))

            verify(exactly = 1) {
                sync.recordOutbound("new", SampleEntity.user, thread.workspace, null, thread, thread.subject.orEmpty(), "내용", "<html/>", thread.counterpartEmail)
            }
        }

        it("허용 도메인 밖 상대면 보내지 않는다") {
            every { messageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread) } returns listOf(message(1, MailDirection.INBOUND))

            shouldThrow<CustomException> { sut(MailRecipientGuard("kio-school.com")).reply("admin", 1, ReplyMailRequestBody(1, "내용")) }
                .errorCode shouldBe ErrorCode.MAIL_RECIPIENT_UNAVAILABLE
            verify { client wasNot Called }
        }

        it("없는 대화면 MAIL_THREAD_NOT_FOUND") {
            every { threadRepository.findByIdOrNull(404) } returns null

            shouldThrow<CustomException> { sut().reply("admin", 404, ReplyMailRequestBody(1, "내용")) }
                .errorCode shouldBe ErrorCode.MAIL_THREAD_NOT_FOUND
        }
    }

    describe("preview") {
        it("실제 메일과 같은 HTML을 돌려준다") {
            sut().preview(MailPreviewRequestBody("<p>내용</p>")).html shouldBe "<html/>"
        }
    }

    describe("delete") {
        it("Resend 삭제가 실패하면 MAIL_SEND_FAILURE를 던진다") {
            every { purge.purgeOne(1) } throws ResendSendException("x")

            shouldThrow<CustomException> { sut().delete(1) }.errorCode shouldBe ErrorCode.MAIL_SEND_FAILURE
        }
    }

    describe("downloadAttachment") {
        val attachment = MailAttachment(MailMessage(resendEmailId = "e1"), "a1", "통장 사본.pdf", 10)

        it("없는 첨부면 MAIL_ATTACHMENT_UNAVAILABLE") {
            every { attachmentRepository.findByIdOrNull(1) } returns null

            shouldThrow<CustomException> { sut().downloadAttachment(1, MockHttpServletResponse()) }
                .errorCode shouldBe ErrorCode.MAIL_ATTACHMENT_UNAVAILABLE
        }

        it("Resend URL이 없으면 MAIL_ATTACHMENT_UNAVAILABLE") {
            every { attachmentRepository.findByIdOrNull(1) } returns attachment
            every { client.attachmentDownloadUrl("e1", "a1") } returns null

            shouldThrow<CustomException> { sut().downloadAttachment(1, MockHttpServletResponse()) }
                .errorCode shouldBe ErrorCode.MAIL_ATTACHMENT_UNAVAILABLE
        }

        it("임시 URL이 만료됐으면 MAIL_ATTACHMENT_UNAVAILABLE") {
            every { attachmentRepository.findByIdOrNull(1) } returns attachment
            every { client.attachmentDownloadUrl("e1", "a1") } returns "https://cdn/x"
            every { downloader.download("https://cdn/x", any()) } returns false

            val response = MockHttpServletResponse()
            shouldThrow<CustomException> { sut().downloadAttachment(1, response) }
                .errorCode shouldBe ErrorCode.MAIL_ATTACHMENT_UNAVAILABLE
            response.getHeader("Content-Disposition") shouldBe null
        }

        it("브라우저에서 바로 열리지 않게 내려받기 헤더를 붙인다") {
            val response = MockHttpServletResponse()
            every { attachmentRepository.findByIdOrNull(1) } returns attachment
            every { client.attachmentDownloadUrl("e1", "a1") } returns "https://cdn/x"
            every { downloader.download("https://cdn/x", any()) } answers {
                secondArg<(java.io.InputStream) -> Unit>()("파일".byteInputStream())
                true
            }

            sut().downloadAttachment(1, response)

            response.getHeader("Content-Disposition").orEmpty() shouldStartWith "attachment"
            response.getHeader("Content-Disposition").orEmpty() shouldContain "UTF-8''"
            response.getHeader("X-Content-Type-Options") shouldBe "nosniff"
            response.contentType shouldBe "application/octet-stream"
            response.contentAsByteArray.toString(Charsets.UTF_8) shouldBe "파일"
        }
    }
})
