package com.kioschool.kioschoolapi.domain.mailbox.campaign.repository

import com.kioschool.kioschoolapi.domain.mailbox.campaign.entity.MailCampaign
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MailCampaignRepository : JpaRepository<MailCampaign, Long>
