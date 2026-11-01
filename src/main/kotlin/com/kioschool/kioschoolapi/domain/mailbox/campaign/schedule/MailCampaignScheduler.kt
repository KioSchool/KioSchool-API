package com.kioschool.kioschoolapi.domain.mailbox.campaign.schedule

import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MailCampaignScheduler(
    private val mailCampaignSender: MailCampaignSender,
) {
    // 전날 남은 곳과 발송 시간 밖에 시작한 곳을 이어 보낸다
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    fun sendDue() {
        mailCampaignSender.sendDue()
    }
}
