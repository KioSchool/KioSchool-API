package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.resend.dto.ResendThreadEmail
import com.kioschool.kioschoolapi.global.resend.service.ResendInboxClient
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

class MailSyncServiceTest : DescribeSpec({
    val client = mockk<ResendInboxClient>()
    val writer = mockk<MailThreadWriter>()
    val sut = MailSyncService(client, writer)
    val emails = emptyList<ResendThreadEmail>()

    afterTest { clearAllMocks() }

    describe("syncThread") {
        it("Resend 조회는 트랜잭션 밖에서 하고 결과를 writer에 넘긴다") {
            every { client.listThreadEmails("t1") } returns emails
            every { writer.upsert("t1", emails) } returns MailSyncResult(2)

            sut.syncThread("t1").newInboundCount shouldBe 2

            verifyOrder {
                client.listThreadEmails("t1")
                writer.upsert("t1", emails)
            }
            MailSyncService::class.java.getMethod("syncThread", String::class.java)
                .isAnnotationPresent(Transactional::class.java) shouldBe false
        }

        it("unique 충돌이 나면 한 번 다시 시도해 성공한다") {
            every { client.listThreadEmails("t1") } returns emails
            every { writer.upsert("t1", emails) } throws DataIntegrityViolationException("dup") andThen MailSyncResult(0)

            sut.syncThread("t1").newInboundCount shouldBe 0

            verify(exactly = 2) { writer.upsert("t1", emails) }
            verify(exactly = 1) { client.listThreadEmails("t1") }
        }

        it("두 번째도 충돌하면 예외를 그대로 던진다") {
            every { client.listThreadEmails("t1") } returns emails
            every { writer.upsert("t1", emails) } throws DataIntegrityViolationException("dup")

            shouldThrow<DataIntegrityViolationException> { sut.syncThread("t1") }
        }
    }

    describe("recordOutbound") {
        it("웹훅과 동시에 행을 만들어 충돌하면 다시 시도한다") {
            val args = arrayOf<Any?>("m1", SampleEntity.user, null, null, null, "s", "t", "h", "a@b.com")
            every { writer.recordOutbound(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws
                DataIntegrityViolationException("dup") andThen Unit

            sut.recordOutbound(
                args[0] as String, SampleEntity.user, null, null, null,
                args[5] as String, args[6] as String, args[7] as String, args[8] as String
            )

            verify(exactly = 2) { writer.recordOutbound("m1", SampleEntity.user, null, null, null, "s", "t", "h", "a@b.com") }
        }
    }
})
