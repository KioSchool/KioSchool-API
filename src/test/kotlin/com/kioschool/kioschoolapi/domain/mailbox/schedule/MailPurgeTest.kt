package com.kioschool.kioschoolapi.domain.mailbox.schedule

import com.kioschool.kioschoolapi.domain.mailbox.repository.MailMessageRepository
import com.kioschool.kioschoolapi.domain.mailbox.repository.MailThreadRepository
import com.kioschool.kioschoolapi.domain.mailbox.service.MailThreadWriter
import com.kioschool.kioschoolapi.domain.mailbox.service.MailPurgeService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import com.kioschool.kioschoolapi.global.resend.service.ResendSendException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.data.repository.findByIdOrNull

class MailPurgeTest : DescribeSpec({
    val repository = mockk<MailThreadRepository>(relaxed = true)
    val client = mockk<ResendInboxClient>(relaxed = true)
    val writer = mockk<MailThreadWriter>(relaxed = true)
    val purgeService = MailPurgeService(repository, client, writer)

    afterTest { clearAllMocks() }

    describe("MailPurgeService.purgeOne") {
        it("Resend를 먼저 지우고 성공한 대화만 DB에서 지운다") {
            val thread = SampleEntity.newMailThread(resendThreadId = "t1")
            every { repository.findByIdOrNull(1) } returns thread

            purgeService.purgeOne(1)

            verifyOrder {
                client.deleteThread("t1")
                writer.delete(1)
            }
        }

        it("Resend 삭제가 실패하면 DB 행을 지우지 않는다") {
            val thread = SampleEntity.newMailThread(resendThreadId = "t1")
            every { repository.findByIdOrNull(1) } returns thread
            every { client.deleteThread("t1") } throws ResendSendException("x")

            shouldThrow<ResendSendException> { purgeService.purgeOne(1) }

            verify(exactly = 0) { writer.delete(any()) }
        }
    }

    describe("MailPurgeScheduler.purgeExpired") {
        val service = mockk<MailPurgeService>(relaxed = true)
        val messageRepository = mockk<MailMessageRepository>(relaxed = true)
        val sut = MailPurgeScheduler(repository, service, messageRepository, 365)

        it("Resend 실패는 그 대화만 건너뛰고 다음 대화를 처리한다") {
            val first = SampleEntity.newMailThread(resendThreadId = "t1")
            val second = SampleEntity.newMailThread(resendThreadId = "t2")
            every { repository.findAllByPurgeAtBefore(any(), any()) } returns listOf(first, second)
            every { service.purgeOne(first.id) } throws ResendSendException("x") andThen Unit

            sut.purgeExpired()

            verify(exactly = 2) { service.purgeOne(any()) }
        }

        it("대화에 연결되지 않은 채 보유 기간이 지난 메일을 지운다") {
            every { repository.findAllByPurgeAtBefore(any(), any()) } returns emptyList()
            val before = slot<java.time.LocalDateTime>()
            every { messageRepository.deleteOrphansCreatedBefore(capture(before)) } returns 2

            sut.purgeExpired()

            val days = java.time.Duration.between(before.captured, java.time.LocalDateTime.now()).toDays()
            days shouldBe 365
        }

        it("만료 안 된 대화는 건드리지 않는다") {
            every { repository.findAllByPurgeAtBefore(any(), any()) } returns emptyList()

            sut.purgeExpired()

            verify { service wasNot Called }
        }
    }
})
