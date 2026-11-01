package com.kioschool.kioschoolapi.domain.mailbox.campaign.repository

import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaign
import com.kioschool.kioschoolapi.domain.mailbox.campaign.enums.MailCampaignStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MailCampaignRepository : JpaRepository<MailCampaign, Long> {
    fun findAllByStatus(status: MailCampaignStatus): List<MailCampaign>

    fun findAllByPurgeAtBeforeAndStatusNot(
        purgeAt: LocalDateTime,
        status: MailCampaignStatus,
        pageable: Pageable,
    ): List<MailCampaign>

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<MailCampaign>
}
