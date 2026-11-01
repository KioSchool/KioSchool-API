package com.kioschool.kioschoolapi.global.resend.service

import com.kioschool.kioschoolapi.global.resend.api.ResendInboxApi
import com.kioschool.kioschoolapi.global.resend.dto.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

@Component
class ResendInboxClient(
    @Value("\${resend.api-key}")
    apiKey: String,
    @Value("\${mail.inbox-id}")
    private val inboxId: String,
    private val resendInboxApi: ResendInboxApi,
    private val mailRecipientGuard: MailRecipientGuard,
) {
    private val authorization = "Bearer $apiKey"

    fun sendNew(
        to: String,
        subject: String,
        html: String,
        text: String,
        headers: Map<String, String>,
        idempotencyKey: String,
    ): String {
        if (!mailRecipientGuard.isAllowed(to)) {
            throw ResendSendException("허용되지 않은 수신 도메인")
        }

        val draft = successBody(
            resendInboxApi.createDraft(
                authorization,
                "$idempotencyKey-draft",
                inboxId,
                ResendCreateDraftRequest(to, subject.replace(LINE_BREAKS, " "), html, text)
            )
        )
        return successBody(
            resendInboxApi.sendDraft(
                authorization,
                "$idempotencyKey-send",
                inboxId,
                draft.id,
                ResendSendDraftRequest(headers)
            )
        ).id
    }

    fun reply(threadId: String, emailId: String, html: String, text: String, idempotencyKey: String): String {
        return successBody(
            resendInboxApi.reply(
                authorization,
                idempotencyKey,
                inboxId,
                threadId,
                emailId,
                ResendReplyRequest(html, text)
            )
        ).id
    }

    fun listThreadEmails(threadId: String): List<ResendThreadEmail> {
        val emails = mutableListOf<ResendThreadEmail>()
        var after: String? = null
        do {
            val page = successBody(resendInboxApi.listThreadEmails(authorization, inboxId, threadId, PAGE_SIZE, after))
            emails += page.data
            after = page.data.lastOrNull()?.id
        } while (page.hasMore && after != null)
        return emails
    }

    fun deleteThread(threadId: String) {
        val response = execute(resendInboxApi.deleteThread(authorization, inboxId, threadId))
        if (response.code() != NOT_FOUND) ensureSuccess(response)
    }

    fun attachmentDownloadUrl(emailId: String, attachmentId: String): String? {
        val response = execute(resendInboxApi.getReceivedAttachment(authorization, emailId, attachmentId))
        if (response.code() == NOT_FOUND) return null
        ensureSuccess(response)
        return response.body()?.downloadUrl
    }

    private fun <T> successBody(call: Call<T>): T {
        val response = execute(call)
        ensureSuccess(response)
        return response.body() ?: throw ResendSendException("Resend 응답 본문 없음")
    }

    private fun <T> execute(call: Call<T>): Response<T> {
        return try {
            call.execute()
        } catch (e: IOException) {
            throw ResendSendException("Resend 호출 실패", e)
        }
    }

    private fun ensureSuccess(response: Response<*>) {
        if (response.isSuccessful) return
        if (response.code() == TOO_MANY_REQUESTS) throw ResendDailyLimitException()
        throw ResendSendException("Resend 응답 ${response.code()}")
    }

    companion object {
        private const val PAGE_SIZE = 100
        private const val NOT_FOUND = 404
        private const val TOO_MANY_REQUESTS = 429
        private val LINE_BREAKS = Regex("[\\r\\n]+")
    }
}
