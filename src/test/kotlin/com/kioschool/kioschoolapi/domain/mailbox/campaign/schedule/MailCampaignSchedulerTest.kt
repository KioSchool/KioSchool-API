package com.kioschool.kioschoolapi.domain.mailbox.campaign.schedule

import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignSender
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.springframework.scheduling.annotation.Scheduled

class MailCampaignSchedulerTest : DescribeSpec({
    describe("sendDue") {
        it("다른 예약 작업을 막지 않도록 비동기로 보낸다") {
            val sender = mockk<MailCampaignSender>(relaxed = true)

            MailCampaignScheduler(sender).sendDue()

            verify(exactly = 1) { sender.sendDueAsync() }
            verify(exactly = 0) { sender.sendDue(any()) }
        }

        it("발송 시간 안에 10분마다 돌아 재시작으로 끊겨도 이어 보낸다") {
            val scheduled = MailCampaignScheduler::class.java.getMethod("sendDue").getAnnotation(Scheduled::class.java)

            scheduled.cron shouldBe "0 */10 10-19 * * *"
            scheduled.zone shouldBe "Asia/Seoul"
        }
    }
})
