package com.kioschool.kioschoolapi.domain.mailbox.campaign.repository

import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaign
import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaignRecipient
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MailCampaignRecipientRepository : JpaRepository<MailCampaignRecipient, Long> {
    // SKIP LOCKED: 같은 수신자를 두 번 잡지 않는다. 멈춘 발송의 수신자는 고르지 않는다.
    @Query(
        value = """
        SELECT r.* FROM mail_campaign_recipient r
        JOIN mail_campaign c ON c.id = r.campaign_id
        WHERE r.status = 'PENDING' AND c.status = 'SENDING'
        ORDER BY r.id
        LIMIT 1
        FOR UPDATE OF r SKIP LOCKED
        """,
        nativeQuery = true,
    )
    fun findNextPendingForUpdate(): MailCampaignRecipient?

    @Query(
        """
        SELECT COUNT(r) FROM MailCampaignRecipient r
        WHERE r.claimedAt >= :start AND r.claimedAt < :end AND r.status IN :statuses
        """
    )
    fun countAttempts(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("statuses") statuses: Collection<MailRecipientStatus>,
    ): Long

    fun findAllByStatusAndClaimedAtBefore(status: MailRecipientStatus, claimedAt: LocalDateTime): List<MailCampaignRecipient>

    @Query(
        """
        SELECT COUNT(r) FROM MailCampaignRecipient r
        WHERE r.campaign = :campaign
          AND r.status IN (
            com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus.PENDING,
            com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailRecipientStatus.SENDING
          )
        """
    )
    fun countUnfinished(@Param("campaign") campaign: MailCampaign): Long

    fun findByResendEmailId(resendEmailId: String): MailCampaignRecipient?

    // 행: [상태, 수]
    @Query("SELECT r.status, COUNT(r) FROM MailCampaignRecipient r WHERE r.campaign = :campaign GROUP BY r.status")
    fun countByCampaignGroupByStatus(@Param("campaign") campaign: MailCampaign): List<Array<Any>>

    fun findAllByCampaignOrderByIdAsc(campaign: MailCampaign, pageable: Pageable): Page<MailCampaignRecipient>

    fun findAllByCampaignAndStatus(campaign: MailCampaign, status: MailRecipientStatus): List<MailCampaignRecipient>
}
