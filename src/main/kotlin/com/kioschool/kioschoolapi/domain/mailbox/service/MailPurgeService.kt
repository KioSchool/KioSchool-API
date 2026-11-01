package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.repository.MailThreadRepository
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class MailPurgeService(
    private val mailThreadRepository: MailThreadRepository,
    private val resendInboxClient: ResendInboxClient,
) {
    // Resend가 실패하면 DB 행을 남겨 다음에 다시 시도한다. Resend 호출은 트랜잭션 밖에서 한다.
    fun purgeOne(threadId: Long) {
        val thread = mailThreadRepository.findByIdOrNull(threadId) ?: return
        resendInboxClient.deleteThread(thread.resendThreadId)
        mailThreadRepository.delete(thread)
    }
}
