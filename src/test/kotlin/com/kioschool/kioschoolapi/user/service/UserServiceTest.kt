package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.account.entity.Bank
import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.repository.AcquisitionSurveyRepository
import com.kioschool.kioschoolapi.domain.user.repository.CustomUserRepository
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import com.kioschool.kioschoolapi.global.common.enums.UserAccountFilter
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class UserServiceTest : DescribeSpec({
    val repository = mockk<UserRepository>()
    val customUserRepository = mockk<CustomUserRepository>()
    val acquisitionSurveyRepository = mockk<AcquisitionSurveyRepository>()
    val passwordEncoder = mockk<PasswordEncoder>()
    val emailService = mockk<EmailService>()

    val sut = UserService(
        repository,
        customUserRepository,
        acquisitionSurveyRepository,
        passwordEncoder,
        emailService
    )

    beforeTest {
        mockkObject(repository)
        mockkObject(customUserRepository)
        mockkObject(acquisitionSurveyRepository)
        mockkObject(passwordEncoder)
        mockkObject(emailService)
    }

    afterTest {
        clearAllMocks()
    }

    describe("checkPassword") {
        it("should throw CustomException(LOGIN_FAILED) when password is not matched") {
            // Given
            val user = User(
                loginId = "test",
                loginPassword = "test",
                name = "test",
                email = "test@test.com",
                role = UserRole.ADMIN,
                accountUrl = "test",
                members = mutableListOf()
            )
            val loginPassword = "wrong password"

            // Mock
            every { passwordEncoder.matches(loginPassword, user.loginPassword) } returns false

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkPassword(user, loginPassword)
            }
            ex.errorCode shouldBe ErrorCode.LOGIN_FAILED
        }

        it("should not throw CustomException(LOGIN_FAILED) when password is matched") {
            // Given
            val user = User(
                loginId = "test",
                loginPassword = "test",
                name = "test",
                email = "test@test.com",
                role = UserRole.ADMIN,
                accountUrl = "test",
                members = mutableListOf()
            )
            val loginPassword = "test"

            // Mock
            every { passwordEncoder.matches(loginPassword, user.loginPassword) } returns true

            // Act & Assert
            sut.checkPassword(user, loginPassword)
        }
    }

    describe("saveUser") {
        it("should save user") {
            val user = SampleEntity.user

            // Mock
            every { repository.save(user) } returns user

            // Act
            sut.saveUser(user)

            // Assert
            verify { repository.save(user) }
        }
    }

    describe("saveUser with parameters") {
        it("should save user") {
            val loginId = "test"
            val loginPassword = "test"
            val name = "test"
            val email = "test@test.com"

            // Mock
            every { repository.save(any<User>()) } returns SampleEntity.user
            every { passwordEncoder.encode(loginPassword) } returns "encoded password"

            // Act
            sut.saveUser(loginId, loginPassword, name, email) shouldBe SampleEntity.user

            // Assert
            verify { repository.save(any<User>()) }
        }
    }

    describe("hasAcquisitionSurvey") {
        it("should return false when the user has no survey row") {
            val user = SampleEntity.user
            every { acquisitionSurveyRepository.findByUser(user) } returns null

            sut.hasAcquisitionSurvey(user) shouldBe false
        }

        it("should return true when a survey row exists") {
            val user = SampleEntity.user
            every { acquisitionSurveyRepository.findByUser(user) } returns AcquisitionSurvey(user = user)

            sut.hasAcquisitionSurvey(user) shouldBe true
        }

        it("should return true even when the row was skipped (channel is null)") {
            val user = SampleEntity.user
            every {
                acquisitionSurveyRepository.findByUser(user)
            } returns AcquisitionSurvey(user = user, channel = null, channelEtc = null, context = "source=naver")

            sut.hasAcquisitionSurvey(user) shouldBe true
        }
    }

    describe("saveAcquisitionSurvey") {
        it("should persist acquisition info on the survey entity") {
            val user = SampleEntity.user
            val savedSlot = slot<AcquisitionSurvey>()
            every { acquisitionSurveyRepository.findByUser(user) } returns null
            every { acquisitionSurveyRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            sut.saveAcquisitionSurvey(user, AcquisitionChannel.SENIOR_HANDOVER, null, "source=instagram")

            savedSlot.captured.user shouldBe user
            savedSlot.captured.channel shouldBe AcquisitionChannel.SENIOR_HANDOVER
            savedSlot.captured.channelEtc shouldBe null
            savedSlot.captured.context shouldBe "source=instagram"
        }

        it("should overwrite the existing survey when called twice for the same user") {
            val user = SampleEntity.user
            val existingSurvey = AcquisitionSurvey(
                user = user,
                channel = AcquisitionChannel.SEARCH,
                channelEtc = null,
                context = "first call"
            )
            val savedSlot = slot<AcquisitionSurvey>()
            every { acquisitionSurveyRepository.findByUser(user) } returns existingSurvey
            every { acquisitionSurveyRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

            sut.saveAcquisitionSurvey(user, AcquisitionChannel.ETC, "\uce5c\uad6c \ucd94\ucc9c", "second call")

            savedSlot.captured shouldBe existingSurvey
            savedSlot.captured.channel shouldBe AcquisitionChannel.ETC
            savedSlot.captured.channelEtc shouldBe "\uce5c\uad6c \ucd94\ucc9c"
            savedSlot.captured.context shouldBe "second call"
            verify(exactly = 1) { acquisitionSurveyRepository.save(any()) }
        }
    }

    describe("validateLoginId") {
        it("should throw CustomException(DUPLICATE_LOGIN_ID) when loginId is duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.validateLoginId(loginId)
            }
            ex.errorCode shouldBe ErrorCode.DUPLICATE_LOGIN_ID
        }

        it("should not throw CustomException(DUPLICATE_LOGIN_ID) when loginId is not duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            sut.validateLoginId(loginId)
        }
    }

    describe("validateEmail") {
        it("should throw CustomException(EMAIL_NOT_VERIFIED) when email is not verified") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns false

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.validateEmail(email)
            }
            ex.errorCode shouldBe ErrorCode.EMAIL_NOT_VERIFIED

            verify { emailService.isRegisterEmailVerified(email) }
            verify(exactly = 0) { repository.findByEmail(email) }
        }

        it("should throw CustomException(DUPLICATE_EMAIL) when email is duplicated") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returns SampleEntity.user

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.validateEmail(email)
            }
            ex.errorCode shouldBe ErrorCode.DUPLICATE_EMAIL

            verify { emailService.isRegisterEmailVerified(email) }
            verify { repository.findByEmail(email) }
        }

        it("should not throw CustomException when email is verified and not duplicated") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returns null

            // Act & Assert
            sut.validateEmail(email)

            verify { emailService.isRegisterEmailVerified(email) }
            verify { repository.findByEmail(email) }
        }
    }

    describe("isDuplicateLoginId") {
        it("should return true when loginId is duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            sut.isDuplicateLoginId(loginId) shouldBe true
        }

        it("should return false when loginId is not duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            sut.isDuplicateLoginId(loginId) shouldBe false
        }
    }

    describe("getUser") {
        it("should return user when user exists") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            sut.getUser(loginId) shouldBe SampleEntity.user
        }

        it("should throw CustomException(USER_NOT_FOUND) when user does not exist") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.getUser(loginId)
            }
            ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
        }
    }

    describe("getUserByEmail") {
        it("should return user when user exists") {
            val email = "test@test.com"

            // Mock
            every { repository.findByEmail(email) } returns SampleEntity.user

            // Act & Assert
            sut.getUserByEmail(email) shouldBe SampleEntity.user

            verify { repository.findByEmail(email) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user does not exist") {
            val email = "test@test.com"

            // Mock
            every { repository.findByEmail(email) } returns null

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.getUserByEmail(email)
            }
            ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
        }
    }

    describe("getAllUsers") {
        it("should pass keyword and account filter to customUserRepository") {
            val keyword = "test"
            val accountFilter = UserAccountFilter.NOT_CONNECTED
            val page = 0
            val size = 10

            every {
                customUserRepository.findAllByCondition(keyword, emptySet(), accountFilter, PageRequest.of(page, size))
            } returns PageImpl(listOf(SampleEntity.user))

            val result = sut.getAllUsers(keyword, emptySet(), accountFilter, page, size)

            result.content.first().id shouldBe SampleEntity.user.id
            verify { customUserRepository.findAllByCondition(keyword, emptySet(), accountFilter, PageRequest.of(page, size)) }
        }
    }

    describe("isSuperAdminUser") {
        it("should return true when user is super admin") {
            val username = "test"
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            // Mock
            every { repository.findByLoginId(username) } returns user

            // Act & Assert
            sut.isSuperAdminUser(username) shouldBe true
        }

        it("should return false when user is not super admin") {
            val username = "test"
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            // Mock
            every { repository.findByLoginId(username) } returns user

            // Act & Assert
            sut.isSuperAdminUser(username) shouldBe false
        }
    }

    describe("checkHasSuperAdminPermission") {
        it("should not throw exception when user is super admin") {
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            // Act & Assert
            sut.checkHasSuperAdminPermission(user)
        }

        it("should throw exception when user is not super admin") {
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkHasSuperAdminPermission(user)
            }
            ex.errorCode shouldBe ErrorCode.NO_PERMISSION
        }
    }

    describe("removeAmountQueryFromAccountUrl") {
        it("should remove amount query from account url") {
            val accountUrl = "test?amount=100&something=else"

            // Act & Assert
            sut.removeAmountQueryFromAccountUrl(accountUrl) shouldBe "test?something=else"
        }
    }

    describe("checkEmailAddress") {
        it("should not throw exception when email is matched") {
            val email = "test@test.com"
            val user = SampleEntity.user
            user.email = email

            // Act & Assert
            sut.checkEmailAddress(user, email)
        }

        it("should throw CustomException(USER_NOT_FOUND) when email is not matched") {
            val email = "test@test.com"
            val wrongEmail = "wrong@wrong.com"
            val user = SampleEntity.user
            user.email = email

            // Act & Assert
            val ex = shouldThrow<CustomException> {
                sut.checkEmailAddress(user, wrongEmail)
            }
            ex.errorCode shouldBe ErrorCode.USER_NOT_FOUND
        }
    }

    describe("withdraw") {
        fun withdrawingUser() = User(
            loginId = "kimop",
            loginPassword = "old-hash",
            name = "김운영",
            email = "kim@korea.ac.kr",
            role = UserRole.ADMIN,
            accountUrl = "https://toss.me/kim",
            account = Account(bank = Bank(name = "우리은행", code = "020"), accountNumber = "1002", accountHolder = "김운영"),
            members = mutableListOf()
        )

        it("erases personal data but keeps the row for workspaces and orders") {
            val user = withdrawingUser()
            val survey = AcquisitionSurvey(user = user, channel = AcquisitionChannel.SAME_SCHOOL)
            val now = LocalDateTime.of(2026, 10, 7, 12, 0)

            every { acquisitionSurveyRepository.findByUser(user) } returns survey
            every { acquisitionSurveyRepository.delete(survey) } just Runs
            every { passwordEncoder.encode(any()) } returns "random-hash"
            every { repository.save(user) } returns user

            sut.withdraw(user, now)

            user.loginId shouldStartWith "withdrawn-${user.id}-"
            user.loginPassword shouldBe "random-hash"
            user.name shouldBe "탈퇴한 회원"
            user.email shouldBe "withdrawn-${user.id}@korea.ac.kr"
            user.account shouldBe null
            user.accountUrl shouldBe null
            user.withdrawnAt shouldBe now
            verify { acquisitionSurveyRepository.delete(survey) }
            verify { repository.save(user) }
            verify(exactly = 0) { repository.delete(any()) }
        }

        // 가입 아이디는 20자까지라 더 길면 새 가입자와 겹치지 않는다
        it("uses a login id longer than any id a new user can register") {
            val user = withdrawingUser()

            every { acquisitionSurveyRepository.findByUser(user) } returns null
            every { passwordEncoder.encode(any()) } returns "random-hash"
            every { repository.save(user) } returns user

            sut.withdraw(user)

            (user.loginId.length > 20) shouldBe true
            verify(exactly = 0) { acquisitionSurveyRepository.delete(any()) }
        }

        // 학교 통계는 이메일 도메인으로 묶는다. 주소는 지우고 도메인만 남긴다
        it("keeps only the school domain of the email") {
            val user = withdrawingUser().apply { email = "kim.op@gs.anyang.ac.kr" }

            every { acquisitionSurveyRepository.findByUser(user) } returns null
            every { passwordEncoder.encode(any()) } returns "random-hash"
            every { repository.save(user) } returns user

            sut.withdraw(user)

            user.email shouldBe "withdrawn-${user.id}@gs.anyang.ac.kr"
        }

        it("leaves the email empty when there was no valid address") {
            listOf(null, "not-an-email", "trailing@").forEach { original ->
                val user = withdrawingUser().apply { email = original }

                every { acquisitionSurveyRepository.findByUser(user) } returns null
                every { passwordEncoder.encode(any()) } returns "random-hash"
                every { repository.save(user) } returns user

                sut.withdraw(user)

                user.email shouldBe null
            }
        }
    }

    describe("savePassword") {
        it("should save password") {
            val user = SampleEntity.user
            val password = "test"
            val encodedPassword = "encoded password"

            // Mock
            every { passwordEncoder.encode(password) } returns encodedPassword
            every { repository.save(user) } returns user

            // Act & Assert
            sut.savePassword(user, password).loginPassword shouldBe encodedPassword

            verify { repository.save(user) }
        }
    }
})