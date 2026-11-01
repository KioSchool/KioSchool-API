package com.kioschool.kioschoolapi.domain.mailbox.repository

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MailThreadRepository : JpaRepository<MailThread, Long> {
    fun findByResendThreadId(resendThreadId: String): MailThread?

    fun findAllByPurgeAtBefore(purgeAt: LocalDateTime, pageable: Pageable): List<MailThread>

    /**
     * 메일함 목록. 답장이 오기 전의 여러 곳 발송 대화는 빼고, 주점 상세에서는 그 주점 대화를 모두 보여준다.
     * 스팸은 스팸 필터에서만 보인다. 상태 조건은 [MailThread.status]와 같아야 한다.
     */
    @Query(
        """
        select t from MailThread t
        where (
            (:workspaceId is null and (t.fromCampaign = false or t.hasInbound = true))
            or t.workspace.id = :workspaceId
        )
        and (
            (:status is null and t.spamAt is null)
            or (:status = 'SPAM' and t.spamAt is not null)
            or (:status = 'RESOLVED' and t.spamAt is null and t.resolvedAt is not null)
            or (:status = 'NEEDS_REPLY' and t.spamAt is null and t.resolvedAt is null
                and t.lastDirection = com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection.INBOUND)
            or (:status = 'WAITING' and t.spamAt is null and t.resolvedAt is null
                and t.lastDirection = com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection.OUTBOUND)
        )
        """
    )
    fun search(
        @Param("status") status: String?,
        @Param("workspaceId") workspaceId: Long?,
        pageable: Pageable,
    ): Page<MailThread>
}
