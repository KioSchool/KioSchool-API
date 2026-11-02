package com.kioschool.kioschoolapi.domain.mailbox.entity

import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class MailThreadTest : DescribeSpec({
    val noon = LocalDateTime.of(2026, 11, 1, 12, 0)

    describe("status") {
        it("spamAt이 있으면 다른 값과 상관없이 SPAM") {
            val sut = SampleEntity.newMailThread(lastDirection = MailDirection.INBOUND, resolvedAt = noon, spamAt = noon)

            sut.status() shouldBe MailThreadStatus.SPAM
        }

        it("resolvedAt이 있으면 RESOLVED") {
            val sut = SampleEntity.newMailThread(lastDirection = MailDirection.INBOUND, resolvedAt = noon)

            sut.status() shouldBe MailThreadStatus.RESOLVED
        }

        it("마지막이 INBOUND면 NEEDS_REPLY") {
            val sut = SampleEntity.newMailThread(lastDirection = MailDirection.INBOUND)

            sut.status() shouldBe MailThreadStatus.NEEDS_REPLY
        }

        it("마지막이 OUTBOUND면 WAITING") {
            val sut = SampleEntity.newMailThread(lastDirection = MailDirection.OUTBOUND)

            sut.status() shouldBe MailThreadStatus.WAITING
        }
    }

    describe("isUnread") {
        it("lastReadAt이 null이면 true") {
            SampleEntity.newMailThread(lastMessageAt = noon, lastReadAt = null).isUnread() shouldBe true
        }

        it("lastReadAt이 lastMessageAt보다 이르면 true") {
            SampleEntity.newMailThread(lastMessageAt = noon, lastReadAt = noon.minusSeconds(1)).isUnread() shouldBe true
        }

        it("같거나 늦으면 false") {
            SampleEntity.newMailThread(lastMessageAt = noon, lastReadAt = noon).isUnread() shouldBe false
            SampleEntity.newMailThread(lastMessageAt = noon, lastReadAt = noon.plusDays(1)).isUnread() shouldBe false
        }
    }

    describe("renewPurgeAt") {
        it("resolvedAt이 없으면 lastMessageAt에서 365일 뒤로 맞춘다") {
            val sut = SampleEntity.newMailThread(lastMessageAt = noon)

            sut.renewPurgeAt(365)

            sut.purgeAt shouldBe noon.plusDays(365)
        }

        it("lastMessageAt과 resolvedAt 중 늦은 시각에서 365일 뒤로 맞춘다") {
            val resolved = noon.plusDays(10)
            val sut = SampleEntity.newMailThread(lastMessageAt = noon, resolvedAt = resolved)

            sut.renewPurgeAt(365)

            sut.purgeAt shouldBe resolved.plusDays(365)
        }
    }
})
