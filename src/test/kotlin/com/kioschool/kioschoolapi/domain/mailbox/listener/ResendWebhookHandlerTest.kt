package com.kioschool.kioschoolapi.domain.mailbox.listener

import com.kioschool.kioschoolapi.domain.mailbox.campaign.service.MailCampaignQueue
import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncResult
import com.kioschool.kioschoolapi.domain.mailbox.service.MailSyncService
import com.kioschool.kioschoolapi.domain.mailbox.service.MailThreadWriter
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import com.kioschool.kioschoolapi.global.resend.service.ResendSendException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.Duration
import java.time.Instant

class ResendWebhookHandlerTest : DescribeSpec({
    val sync = mockk<MailSyncService>()
    val writer = mockk<MailThreadWriter>(relaxed = true)
    val discord = mockk<DiscordService>(relaxed = true)
    val scheduler = mockk<MailRetryScheduler>()
    val queue = mockk<MailCampaignQueue>(relaxed = true)
    val sut = ResendWebhookHandler(sync, writer, queue, discord, scheduler, java.util.concurrent.Executor { it.run() })
    val scheduled = mutableListOf<Runnable>()
    val delays = mutableListOf<Duration>()

    beforeTest {
        scheduled.clear()
        delays.clear()
        every { scheduler.schedule(any(), any()) } answers {
            scheduled += firstArg<Runnable>()
            delays += Duration.between(Instant.now(), secondArg<Instant>())
        }
    }

    afterTest { clearAllMocks() }

    describe("handle") {
        it("inbox.email.received는 스레드를 동기화하고 새 받은 메일이 있으면 sendNewMail을 부른다") {
            every { sync.syncThread("t1") } returns MailSyncResult(1)

            sut.handle(ResendWebhookEvent("inbox.email.received", "i", "t1", "e1"))

            verify(exactly = 1) { discord.sendNewMail() }
        }

        it("같은 이벤트 재시도로 newInboundCount가 0이면 Discord를 보내지 않는다") {
            every { sync.syncThread("t1") } returns MailSyncResult(0)

            sut.handle(ResendWebhookEvent("inbox.email.received", "i", "t1", "e1"))

            verify(exactly = 0) { discord.sendNewMail() }
        }

        it("inbox.email.sent는 동기화만 한다") {
            every { sync.syncThread("t1") } returns MailSyncResult(0)

            sut.handle(ResendWebhookEvent("inbox.email.sent", "i", "t1", "e1"))

            verify(exactly = 1) { sync.syncThread("t1") }
            verify(exactly = 0) { discord.sendNewMail() }
        }

        it("email.bounced는 해당 메일의 bouncedAt을 채운다") {
            sut.handle(ResendWebhookEvent("email.bounced", null, null, "e1"))

            verify(exactly = 1) { writer.markBounced("e1", any()) }
        }

        it("email.bounced가 수신자 메일이면 그 수신자를 실패로 바꾼다") {
            sut.handle(ResendWebhookEvent("email.bounced", null, null, "e1"))

            verify(exactly = 1) { queue.markBounced("e1") }
        }

        it("모르는 이벤트는 무시한다") {
            sut.handle(ResendWebhookEvent("inbox.draft.created", "i", null, null))

            verify { sync wasNot Called }
            verify { writer wasNot Called }
        }

        it("필요한 ID가 없으면 무시한다") {
            sut.handle(ResendWebhookEvent("inbox.email.received", "i", null, "e1"))
            sut.handle(ResendWebhookEvent("email.bounced", null, null, null))

            verify { sync wasNot Called }
            verify { writer wasNot Called }
        }

        it("처리 중 예외가 나도 다시 던지지 않고 1분 뒤로 다시 시도를 잡는다") {
            every { sync.syncThread("t1") } throws ResendSendException("Resend 응답 500")

            sut.handle(ResendWebhookEvent("inbox.email.received", "i", "t1", "e1"))

            verify(exactly = 0) { discord.sendNewMail() }
            scheduled.size shouldBe 1
            (delays.single().toSeconds() in 50..60) shouldBe true
        }

        it("다시 시도에서 성공하면 새 메일 알림을 보낸다") {
            every { sync.syncThread("t1") } throws ResendSendException("x") andThen MailSyncResult(1)

            sut.handle(ResendWebhookEvent("inbox.email.received", "i", "t1", "e1"))
            scheduled.single().run()

            verify(exactly = 1) { discord.sendNewMail() }
            verify(exactly = 0) { discord.sendMailSyncFailed() }
        }

        it("1분·5분·30분 뒤에도 실패하면 내용 없는 알림을 보낸다") {
            every { sync.syncThread("t1") } throws ResendSendException("x")

            sut.handle(ResendWebhookEvent("inbox.email.received", "i", "t1", "e1"))
            repeat(3) { scheduled[it].run() }

            scheduled.size shouldBe 3
            (delays[1].toMinutes() in 4..5) shouldBe true
            (delays[2].toMinutes() in 29..30) shouldBe true
            verify(exactly = 1) { discord.sendMailSyncFailed() }
            verify(exactly = 4) { sync.syncThread("t1") }
        }

        it("반송 처리가 실패해도 다시 시도한다") {
            every { writer.markBounced("e1", any()) } throws RuntimeException("db") andThen Unit

            sut.handle(ResendWebhookEvent("email.bounced", null, null, "e1"))
            scheduled.single().run()

            verify(exactly = 2) { writer.markBounced("e1", any()) }
        }
    }
})
