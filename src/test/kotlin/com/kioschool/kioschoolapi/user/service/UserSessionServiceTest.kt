package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.domain.user.entity.UserSession
import com.kioschool.kioschoolapi.domain.user.repository.UserSessionRepository
import com.kioschool.kioschoolapi.domain.user.service.SessionRefreshResult
import com.kioschool.kioschoolapi.domain.user.service.SessionRefreshResult.RejectReason
import com.kioschool.kioschoolapi.domain.user.service.UserSessionService
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.Optional

class UserSessionServiceTest : DescribeSpec({
    val repository = mockk<UserSessionRepository>()
    val validity = Duration.ofDays(7)
    val meterRegistry = SimpleMeterRegistry()
    val sut = UserSessionService(repository, validity, meterRegistry)

    val now = LocalDateTime.of(2026, 10, 5, 18, 0)
    val secret = "current-secret"
    val previousSecret = "previous-secret"

    fun hash(value: String) = UserSessionService.hash(value)

    fun count(result: String) =
        meterRegistry.find("kioschool.auth.refresh").tag("result", result).counter()?.count() ?: 0.0

    afterTest {
        clearAllMocks()
        meterRegistry.clear()
    }

    describe("create") {
        it("비밀값은 해시로만 저장하고 {세션 ID}.{비밀값} 형태로 돌려준다") {
            val saved = slot<UserSession>()
            every { repository.save(capture(saved)) } answers {
                SampleEntity.userSession(id = 42L, tokenHash = saved.captured.tokenHash, expiresAt = saved.captured.expiresAt)
            }

            val token = sut.create(SampleEntity.user, now)

            val (sessionId, rawSecret) = token.split(".", limit = 2)
            sessionId shouldBe "42"
            saved.captured.tokenHash shouldBe hash(rawSecret)
            saved.captured.tokenHash shouldNotBe rawSecret
            saved.captured.expiresAt shouldBe now.plus(validity)
        }
    }

    describe("refresh") {
        it("현재 토큰이면 교체하고 만료를 7일 뒤로 늘린다") {
            val session = SampleEntity.userSession(id = 1L, tokenHash = hash(secret), expiresAt = now.plusDays(1))
            every { repository.findByIdForUpdate(1L) } returns session

            val result = sut.refresh("1.$secret", now)

            val rotated = result.shouldBeInstanceOf<SessionRefreshResult.Rotated>()
            rotated.user shouldBe SampleEntity.user
            val newSecret = rotated.refreshToken.substringAfter(".")
            rotated.refreshToken.substringBefore(".") shouldBe "1"
            session.tokenHash shouldBe hash(newSecret)
            session.previousTokenHash shouldBe hash(secret)
            session.rotatedAt shouldBe now
            session.expiresAt shouldBe now.plus(validity)
            count("rotated") shouldBe 1.0
        }

        it("방금 교체된 토큰이면 유예로 access token만 내준다") {
            val session = SampleEntity.userSession(
                tokenHash = hash(secret),
                expiresAt = now.plusDays(7),
                previousTokenHash = hash(previousSecret),
                rotatedAt = now.minusSeconds(10),
            )
            every { repository.findByIdForUpdate(1L) } returns session

            val result = sut.refresh("1.$previousSecret", now)

            result.shouldBeInstanceOf<SessionRefreshResult.Grace>().user shouldBe SampleEntity.user
            session.tokenHash shouldBe hash(secret)
            verify(exactly = 0) { repository.delete(any()) }
            count("grace") shouldBe 1.0
        }

        it("교체된 지 30초가 지난 토큰이 다시 오면 세션을 지운다") {
            val session = SampleEntity.userSession(
                tokenHash = hash(secret),
                expiresAt = now.plusDays(7),
                previousTokenHash = hash(previousSecret),
                rotatedAt = now.minusSeconds(31),
            )
            every { repository.findByIdForUpdate(1L) } returns session
            every { repository.delete(session) } just Runs

            val result = sut.refresh("1.$previousSecret", now)

            result shouldBe SessionRefreshResult.Rejected(RejectReason.REUSED)
            verify { repository.delete(session) }
            count("reused") shouldBe 1.0
        }

        it("어느 해시와도 맞지 않으면 거부만 하고 세션은 지우지 않는다") {
            val session = SampleEntity.userSession(
                tokenHash = hash(secret),
                expiresAt = now.plusDays(7),
                previousTokenHash = hash(previousSecret),
                rotatedAt = now.minusDays(1),
            )
            every { repository.findByIdForUpdate(1L) } returns session

            val result = sut.refresh("1.guessed", now)

            result shouldBe SessionRefreshResult.Rejected(RejectReason.INVALID)
            verify(exactly = 0) { repository.delete(any()) }
        }

        it("만료된 세션은 지우고 거부한다") {
            val session = SampleEntity.userSession(tokenHash = hash(secret), expiresAt = now)
            every { repository.findByIdForUpdate(1L) } returns session
            every { repository.delete(session) } just Runs

            val result = sut.refresh("1.$secret", now)

            result shouldBe SessionRefreshResult.Rejected(RejectReason.EXPIRED)
            verify { repository.delete(session) }
        }

        it("세션이 없으면 거부한다") {
            every { repository.findByIdForUpdate(1L) } returns null

            sut.refresh("1.$secret", now) shouldBe SessionRefreshResult.Rejected(RejectReason.INVALID)
        }

        it("쿠키가 없거나 형식이 틀리면 DB를 보지 않고 거부한다") {
            listOf(null, "", "no-separator", "abc.secret", "1.").forEach { token ->
                sut.refresh(token, now) shouldBe SessionRefreshResult.Rejected(RejectReason.INVALID)
            }
            verify(exactly = 0) { repository.findByIdForUpdate(any()) }
        }
    }

    describe("delete") {
        it("토큰이 맞으면 세션을 지운다") {
            val session = SampleEntity.userSession(tokenHash = hash(secret), expiresAt = now.plusDays(7))
            every { repository.findById(1L) } returns Optional.of(session)
            every { repository.delete(session) } just Runs

            sut.delete("1.$secret", now)

            verify { repository.delete(session) }
        }

        it("세션 ID만 맞힌 요청으로는 지우지 않는다") {
            val session = SampleEntity.userSession(tokenHash = hash(secret), expiresAt = now.plusDays(7))
            every { repository.findById(1L) } returns Optional.of(session)

            sut.delete("1.guessed", now)

            verify(exactly = 0) { repository.delete(any()) }
        }

        it("쿠키가 없으면 아무것도 하지 않는다") {
            sut.delete(null, now)

            verify(exactly = 0) { repository.findById(any()) }
        }
    }

    describe("deleteAllOf") {
        it("그 유저의 모든 세션을 지운다") {
            every { repository.deleteAllByUser(SampleEntity.user) } returns 3

            sut.deleteAllOf(SampleEntity.user)

            verify { repository.deleteAllByUser(SampleEntity.user) }
        }
    }
})
