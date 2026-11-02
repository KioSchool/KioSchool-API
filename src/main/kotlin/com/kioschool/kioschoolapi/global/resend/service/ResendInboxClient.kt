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
    @Value("\${resend.rate-limit-retry-delay-millis:1000}")
    private val rateLimitRetryDelayMillis: Long,
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
        return createAndSendDraft(to, subject, html, text, headers, idempotencyKey)
    }

    // 로그인한 사람의 주소로 보내는 시험 발송은 실제 운영진에게 잘못 나갈 일이 없어 허용 도메인을 보지 않는다
    fun sendToOwnAddress(
        to: String,
        subject: String,
        html: String,
        text: String,
        headers: Map<String, String>,
        idempotencyKey: String,
    ): String = createAndSendDraft(to, subject, html, text, headers, idempotencyKey)

    private fun createAndSendDraft(
        to: String,
        subject: String,
        html: String,
        text: String,
        headers: Map<String, String>,
        idempotencyKey: String,
    ): String {
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

    // 실패한 요청은 Idempotency-Key에 저장되지 않으므로 초당 제한은 같은 키로 다시 시도해도 된다
    private fun <T> execute(call: Call<T>): Response<T> {
        var current = call
        repeat(RATE_LIMIT_ATTEMPTS) {
            val response = executeOnce(current)
            if (response.code() != TOO_MANY_REQUESTS) return response

            val error = response.errorBody()?.string().orEmpty()
            if (QUOTA_ERRORS.any { it in error }) throw ResendDailyLimitException()

            sleep(retryDelayOf(response))
            current = current.clone()
        }
        throw ResendSendException("Resend 응답 $TOO_MANY_REQUESTS")
    }

    private fun <T> executeOnce(call: Call<T>): Response<T> {
        return try {
            call.execute()
        } catch (e: IOException) {
            throw ResendSendException("Resend 호출 실패", e)
        }
    }

    private fun retryDelayOf(response: Response<*>): Long =
        response.headers()["retry-after"]?.toLongOrNull()?.let { minOf(it * 1000, MAX_RETRY_DELAY_MILLIS) }
            ?: rateLimitRetryDelayMillis

    private fun sleep(millis: Long) {
        if (millis > 0) Thread.sleep(millis)
    }

    private fun ensureSuccess(response: Response<*>) {
        if (response.isSuccessful) return
        throw ResendSendException("Resend 응답 ${response.code()} ${errorReasonOf(response)}".trimEnd())
    }

    // 원인을 알 수 있게 Resend가 알려 준 사유를 남긴다. 사유에 받는 주소가 들어 있을 수 있어 가린다.
    private fun errorReasonOf(response: Response<*>): String =
        response.errorBody()?.string().orEmpty()
            .replace(EMAIL_ADDRESS, "***")
            .take(MAX_ERROR_REASON_LENGTH)

    companion object {
        private const val PAGE_SIZE = 100
        private const val NOT_FOUND = 404
        private const val TOO_MANY_REQUESTS = 429
        private const val RATE_LIMIT_ATTEMPTS = 3
        private const val MAX_RETRY_DELAY_MILLIS = 5000L
        private val QUOTA_ERRORS = listOf("daily_quota_exceeded", "monthly_quota_exceeded")
        private val LINE_BREAKS = Regex("[\\r\\n]+")
        private val EMAIL_ADDRESS = Regex("[^\\s\"'<>:,]+@[^\\s\"'<>:,]+")
        private const val MAX_ERROR_REASON_LENGTH = 300
    }
}
