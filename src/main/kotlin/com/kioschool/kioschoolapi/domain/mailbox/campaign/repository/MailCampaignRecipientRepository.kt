package com.kioschool.kioschoolapi.domain.mailbox.campaign.repository

import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaignRecipient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MailCampaignRecipientRepository : JpaRepository<MailCampaignRecipient, Long>
