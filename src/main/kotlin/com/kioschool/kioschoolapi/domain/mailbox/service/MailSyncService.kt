package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class MailSyncService(
    private val resendInboxClient: ResendInboxClient,
    private val mailThreadWriter: MailThreadWriter,
) {
    fun syncThread(resendThreadId: String): MailSyncResult {
        val emails = resendInboxClient.listThreadEmails(resendThreadId)
        return retryOnConflict { mailThreadWriter.upsert(resendThreadId, emails) }
    }

    fun recordOutbound(
        emailId: String,
        sentBy: User?,
        workspace: Workspace?,
        campaignRecipientId: Long?,
        thread: MailThread?,
        subject: String,
        text: String,
        html: String,
        to: String,
    ) {
        retryOnConflict {
            mailThreadWriter.recordOutbound(emailId, sentBy, workspace, campaignRecipientId, thread, subject, text, html, to)
        }
    }

    // 같은 스레드를 다른 요청이 먼저 넣으면 unique 제약에 걸린다. 다시 읽으면 그 행을 찾으므로 한 번만 더 시도한다.
    private fun <T> retryOnConflict(block: () -> T): T {
        return try {
            block()
        } catch (e: DataIntegrityViolationException) {
            block()
        }
    }
}
