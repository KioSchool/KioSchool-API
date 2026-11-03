package com.kioschool.kioschoolapi.domain.mailbox.facade

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailPreviewDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailThreadDetailDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailThreadSummaryDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.MailPreviewRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.ReplyMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.SendMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailAttachmentRepository
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailMessageRepository
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailThreadRepository
import com.kioschool.kioschoolapi.domain.mailbox.service.MailBodyRenderer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailPurgeService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailQuoteBuilder
import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailThreadWriter
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.resend.service.MailRecipientGuard
import com.kioschool.kioschoolapi.global.resend.service.ResendAttachmentDownloader
import com.kioschool.kioschoolapi.global.resend.service.ResendDailyLimitException
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import com.kioschool.kioschoolapi.global.resend.service.ResendSendException
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.util.UUID

// Resend 호출이 트랜잭션에 묶이지 않도록 이 클래스에는 @Transactional을 두지 않는다. DB 변경은 MailThreadWriter가 맡는다.
@Component
class MailFacade(
    private val mailThreadRepository: MailThreadRepository,
    private val mailMessageRepository: MailMessageRepository,
    private val mailAttachmentRepository: MailAttachmentRepository,
    private val mailThreadWriter: MailThreadWriter,
    private val mailSyncService: MailSyncService,
    private val mailPurgeService: MailPurgeService,
    private val mailBodyRenderer: MailBodyRenderer,
    private val mailHtmlSanitizer: MailHtmlSanitizer,
    private val mailQuoteBuilder: MailQuoteBuilder,
    private val resendInboxClient: ResendInboxClient,
    private val resendAttachmentDownloader: ResendAttachmentDownloader,
    private val mailRecipientGuard: MailRecipientGuard,
    private val workspaceService: WorkspaceService,
    private val userService: UserService,
    private val emailService: EmailService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getThreads(status: MailThreadStatus?, workspaceId: Long?, page: Int, size: Int): Page<MailThreadSummaryDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"))
        return mailThreadRepository.search(status?.name, workspaceId, pageable).map { MailThreadSummaryDto.of(it) }
    }

    fun getThread(threadId: Long): MailThreadDetailDto = toDetailDto(mailThreadWriter.markRead(threadId))

    fun sendNew(username: String, body: SendMailRequestBody) {
        val workspace = workspaceService.getWorkspace(body.workspaceId)
        val to = recipientOf(workspace)
        val subject = body.normalizedSubject()
        val content = cleanContent(body.content)
        val html = mailBodyRenderer.render(content)
        val text = mailBodyRenderer.renderText(content)
        val sender = userService.getUser(username)

        val emailId = callResend {
            resendInboxClient.sendNew(to, subject, html, text, emptyMap(), "mail-new-${body.requestId ?: UUID.randomUUID()}")
        }

        record(emailId, sender, workspace, null, subject, text, html, to)
    }

    fun reply(username: String, threadId: Long, body: ReplyMailRequestBody): MailThreadDetailDto {
        val thread = mailThreadRepository.findByIdOrNull(threadId)
            ?: throw CustomException(ErrorCode.MAIL_THREAD_NOT_FOUND)
        val messages = mailMessageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread)
        val latest = messages.lastOrNull()
        if (latest == null || latest.id != body.lastMessageId) throw CustomException(ErrorCode.MAIL_THREAD_STALE)

        // 답장 상대는 이 메일함과 이미 주고받은 주소이므로 허용 도메인을 확인하지 않는다
        val target = messages.lastOrNull { it.direction == MailDirection.INBOUND } ?: latest
        val content = cleanContent(body.content)
        val quote = mailQuoteBuilder.build(latest)
        val html = mailBodyRenderer.renderReply(content, quote?.html)
        val text = listOfNotNull(mailBodyRenderer.renderText(content), quote?.text).joinToString("\n\n")
        val sender = userService.getUser(username)

        val emailId = callResend {
            resendInboxClient.reply(
                thread.resendThreadId,
                target.resendEmailId,
                html,
                text,
                "mail-reply-${thread.id}-${latest.id}-${content.hashCode()}",
            )
        }

        record(emailId, sender, thread.workspace, thread, thread.subject.orEmpty(), text, html, thread.counterpartEmail)
        return getThread(threadId)
    }

    fun preview(body: MailPreviewRequestBody): MailPreviewDto = MailPreviewDto(mailBodyRenderer.render(body.content))

    fun resolve(threadId: Long): MailThreadDetailDto = toDetailDto(mailThreadWriter.resolve(threadId))

    fun markSpam(threadId: Long): MailThreadDetailDto = toDetailDto(mailThreadWriter.markSpam(threadId))

    fun unmarkSpam(threadId: Long): MailThreadDetailDto = toDetailDto(mailThreadWriter.unmarkSpam(threadId))

    fun delete(threadId: Long) {
        callResend { mailPurgeService.purgeOne(threadId) }
    }

    // 임시 URL을 화면에 주지 않고 서버가 받아 전달한다. 파일 이름이 로그에 남지 않도록 응답 객체에 직접 쓴다.
    fun downloadAttachment(attachmentId: Long, response: HttpServletResponse) {
        val attachment = mailAttachmentRepository.findByIdOrNull(attachmentId)
            ?: throw CustomException(ErrorCode.MAIL_ATTACHMENT_UNAVAILABLE)
        val url = callResend {
            resendInboxClient.attachmentDownloadUrl(attachment.message.resendEmailId, attachment.resendAttachmentId)
        } ?: throw CustomException(ErrorCode.MAIL_ATTACHMENT_UNAVAILABLE)

        val downloaded = callResend {
            resendAttachmentDownloader.download(url) { stream ->
                response.contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(attachment.filename))
                response.setHeader(HEADER_NO_SNIFF, "nosniff")
                stream.copyTo(response.outputStream)
            }
        }
        if (!downloaded) throw CustomException(ErrorCode.MAIL_ATTACHMENT_UNAVAILABLE)
    }

    private fun cleanContent(content: String): String {
        val cleaned = mailHtmlSanitizer.clean(content)
        if (mailHtmlSanitizer.isBlank(cleaned)) throw CustomException(ErrorCode.INVALID_INPUT, "내용을 입력해 주세요.")
        if (mailHtmlSanitizer.hasInvalidLink(content)) throw CustomException(ErrorCode.MAIL_TEMPLATE_INVALID)
        return cleaned
    }

    private fun contentDisposition(filename: String?): String {
        val builder = ContentDisposition.attachment()
        if (!filename.isNullOrBlank()) builder.filename(filename, Charsets.UTF_8)
        return builder.build().toString()
    }

    private fun recipientOf(workspace: Workspace): String {
        val owner = workspace.owner
        val email = owner.email
        if (email.isNullOrBlank() || owner.withdrawnAt != null || !mailRecipientGuard.isAllowed(email)) {
            throw CustomException(ErrorCode.MAIL_RECIPIENT_UNAVAILABLE)
        }
        return email
    }

    // 메일은 이미 나갔으므로 기록이 실패해도 오류로 돌려주지 않는다. 내용은 웹훅이 다시 채운다.
    private fun record(
        emailId: String,
        sender: User,
        workspace: Workspace?,
        thread: MailThread?,
        subject: String,
        content: String,
        html: String,
        to: String,
    ) {
        runCatching { mailSyncService.recordOutbound(emailId, sender, workspace, null, thread, subject, content, html, to) }
            .onFailure { log.error("Failed to record outbound mail: emailId={}", emailId, it) }
    }

    private fun <T> callResend(block: () -> T): T {
        return try {
            block()
        } catch (e: ResendDailyLimitException) {
            throw CustomException(ErrorCode.MAIL_DAILY_LIMIT_EXCEEDED, cause = e)
        } catch (e: ResendSendException) {
            log.error("Resend call failed: {}", e.message)
            throw CustomException(ErrorCode.MAIL_SEND_FAILURE, cause = e)
        }
    }

    private fun toDetailDto(thread: MailThread): MailThreadDetailDto {
        val messages = mailMessageRepository.findAllByThreadOrderBySentAtAscIdAsc(thread)
        val schoolName = thread.workspace?.let { emailService.getSchoolResolver().schoolOf(it.owner.email) }
        return MailThreadDetailDto.of(thread, schoolName, messages, mailQuoteBuilder::visibleHtml)
    }

    companion object {
        private const val HEADER_NO_SNIFF = "X-Content-Type-Options"
    }
}
