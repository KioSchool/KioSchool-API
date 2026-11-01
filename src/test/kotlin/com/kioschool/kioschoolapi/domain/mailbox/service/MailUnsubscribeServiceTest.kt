package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class MailUnsubscribeServiceTest : DescribeSpec({
    val repository = mockk<UserRepository>()
    val sut = MailUnsubscribeService(repository)

    fun user(token: String? = null, unsubscribedAt: LocalDateTime? = null) = User(
        loginId = "o", loginPassword = "p", name = "사장", email = "owner@konkuk.ac.kr",
        role = UserRole.ADMIN, members = mutableListOf(),
    ).apply {
        mailUnsubscribeToken = token
        mailUnsubscribedAt = unsubscribedAt
    }

    beforeTest {
        clearAllMocks()
        every { repository.save(any<User>()) } answers { firstArg() }
    }

    describe("tokenFor") {
        it("없으면 무작위 값을 만들어 저장한다") {
            val first = user()
            val second = user()

            val token = sut.tokenFor(first)

            token.length shouldBeGreaterThanOrEqual 64
            first.mailUnsubscribeToken shouldBe token
            sut.tokenFor(second) shouldNotBe token
            verify { repository.save(first) }
        }

        it("있으면 그대로 돌려준다") {
            sut.tokenFor(user(token = "기존")) shouldBe "기존"

            verify(exactly = 0) { repository.save(any<User>()) }
        }
    }

    describe("unsubscribe") {
        it("시각을 남긴다") {
            val owner = user(token = "t")
            every { repository.findByMailUnsubscribeToken("t") } returns owner

            sut.unsubscribe("t")

            owner.mailUnsubscribedAt.shouldNotBeNull()
        }

        it("두 번 불러도 처음 시각을 유지한다") {
            val first = LocalDateTime.of(2026, 11, 1, 12, 0)
            val owner = user(token = "t", unsubscribedAt = first)
            every { repository.findByMailUnsubscribeToken("t") } returns owner

            sut.unsubscribe("t")

            owner.mailUnsubscribedAt shouldBe first
        }

        it("모르는 토큰이면 MAIL_UNSUBSCRIBE_TOKEN_INVALID") {
            every { repository.findByMailUnsubscribeToken("x") } returns null

            shouldThrow<CustomException> { sut.unsubscribe("x") }.errorCode shouldBe ErrorCode.MAIL_UNSUBSCRIBE_TOKEN_INVALID
        }
    }

    describe("find") {
        it("이메일은 가려져 있고 토큰으로 상태를 바꾸지 않는다") {
            val owner = user(token = "t")
            every { repository.findByMailUnsubscribeToken("t") } returns owner

            val info = sut.find("t")

            info.maskedEmail shouldBe "****@konkuk.ac.kr"
            info.unsubscribed shouldBe false
            owner.mailUnsubscribedAt shouldBe null
            verify(exactly = 0) { repository.save(any<User>()) }
        }

        it("모르는 토큰이면 MAIL_UNSUBSCRIBE_TOKEN_INVALID") {
            every { repository.findByMailUnsubscribeToken("x") } returns null

            shouldThrow<CustomException> { sut.find("x") }.errorCode shouldBe ErrorCode.MAIL_UNSUBSCRIBE_TOKEN_INVALID
        }
    }
})
