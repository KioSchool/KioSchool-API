package com.kioschool.kioschoolapi.domain.mailbox.campaign.schedule

import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MailCampaignScheduler(
    private val mailCampaignSender: MailCampaignSender,
) {
    // 전날 남은 곳, 발송 시간 밖에 시작한 곳, 재시작으로 끊긴 곳을 이어 보낸다.
    // 수십 초 이상 걸리므로 다른 예약 작업을 막지 않게 비동기로 넘긴다.
    @Scheduled(cron = "0 */10 10-19 * * *", zone = "Asia/Seoul")
    fun sendDue() {
        mailCampaignSender.sendDueAsync()
    }
}
