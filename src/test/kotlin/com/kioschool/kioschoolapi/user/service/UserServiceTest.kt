package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.user.exception.RegisterException
import com.kioschool.kioschoolapi.user.exception.UserNotFoundException
import com.kioschool.kioschoolapi.user.repository.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest : DescribeSpec({
    val repository = mockk<UserRepository>()
    val emailService = mockk<EmailService>()
    val jwtProvider = mockk<JwtProvider>()
    val passwordEncoder = BCryptPasswordEncoder()
    val mockPasswordEncoder = mockk<PasswordEncoder>()
    val discordService = mockk<DiscordService>()
    val sut =
        UserService(repository, jwtProvider, mockPasswordEncoder, emailService, discordService)

    describe("login") {
        it("should return token when login success") {
            val sampleUser = SampleEntity.user
            val loginId = sampleUser.loginId
            val loginPassword = sampleUser.loginPassword

            every { repository.findByLoginId(loginId) } returnsMany listOf(SampleEntity.user)
            every {
                mockPasswordEncoder.matches(
                    loginPassword,
                    sampleUser.loginPassword
                )
            } returns true
            every { jwtProvider.createToken(sampleUser) } returns "token"

            sut.login(loginId, loginPassword) shouldBe "token"
        }

        it("should throw LoginFailedException when login failed by different password") {
            val loginId = "loginId"
            val loginPassword = "loginPassword"

            every { repository.findByLoginId(loginId) } returnsMany listOf(SampleEntity.user)
            every {
                mockPasswordEncoder.matches(
                    loginPassword,
                    SampleEntity.user.loginPassword
                )
            } returns false

            try {
                sut.login(loginId, loginPassword)
            } catch (e: Exception) {
                e shouldBe LoginFailedException()
            }
        }

        it("should throw UserNotFoundException when login failed by doesn't exist user") {
            val loginId = "loginId"
            val loginPassword = "loginPassword"

            every { repository.findByLoginId(loginId) } returnsMany listOf(null)

            try {
                sut.login(loginId, loginPassword)
            } catch (e: Exception) {
                e shouldBe UserNotFoundException()
            }
        }
    }

    describe("checkPassword") {
        it("should not throw exception when password is same") {
            val sampleUser = SampleEntity.user
            val loginPassword = sampleUser.loginPassword
            sampleUser.loginPassword = passwordEncoder.encode(loginPassword)

            every {
                mockPasswordEncoder.matches(
                    loginPassword,
                    sampleUser.loginPassword
                )
            } returns (passwordEncoder.matches(loginPassword, sampleUser.loginPassword))

            sut.checkPassword(sampleUser, loginPassword)
        }

        it("should throw LoginFailedException when password is different") {
            val sampleUser = SampleEntity.user
            val loginPassword = "differentPassword"

            every {
                mockPasswordEncoder.matches(
                    loginPassword,
                    sampleUser.loginPassword
                )
            } returns (passwordEncoder.matches(loginPassword, sampleUser.loginPassword))

            try {
                sut.checkPassword(sampleUser, loginPassword)
            } catch (e: Exception) {
                e shouldBe LoginFailedException()
            }
        }
    }

    describe("register") {
        it("should return token when register success") {
            val loginId = "newLoginId"
            val loginPassword = "newLoginPassword"
            val name = "newName"
            val email = "newEmail"

            every { repository.findByLoginId(loginId) } returnsMany listOf(null)
            every { repository.findByEmail(email) } returnsMany listOf(null)
            every { emailService.isEmailVerified(email) } returns true
            every { emailService.deleteRegisterCode(email) } returns Unit
            every { repository.save(any()) } returns SampleEntity.user
            every { discordService.sendUserRegister(SampleEntity.user) } returns Unit
            every { mockPasswordEncoder.encode(loginPassword) } returns loginPassword
            every { jwtProvider.createToken(SampleEntity.user) } returns "token"

            sut.register(loginId, loginPassword, name, email) shouldBe "token"
        }

        it("should throw RegisterException when loginId is duplicate") {
            val loginId = "loginId"
            val loginPassword = "loginPassword"
            val name = "name"
            val email = "email"

            every { repository.findByLoginId(loginId) } returnsMany listOf(SampleEntity.user)

            try {
                sut.register(loginId, loginPassword, name, email)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }

        it("should throw RegisterException when email is not verified") {
            val loginId = "loginId"
            val loginPassword = "loginPassword"
            val name = "name"
            val email = "email"

            every { repository.findByLoginId(loginId) } returnsMany listOf(null)
            every { emailService.isEmailVerified(email) } returns false

            try {
                sut.register(loginId, loginPassword, name, email)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }

        it("should throw RegisterException when email is duplicate") {
            val loginId = "loginId"
            val loginPassword = "loginPassword"
            val name = "name"
            val email = "email"

            every { repository.findByLoginId(loginId) } returnsMany listOf(null)
            every { emailService.isEmailVerified(email) } returns false
            every { repository.findByEmail(email) } returnsMany listOf(SampleEntity.user)

            try {
                sut.register(loginId, loginPassword, name, email)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }
    }

    describe("validateLoginId") {
        it("should not throw exception when loginId is not duplicate") {
            val loginId = "loginId"

            every { repository.findByLoginId(loginId) } returnsMany listOf(null)

            sut.validateLoginId(loginId)
        }

        it("should throw RegisterException when loginId is duplicate") {
            val loginId = "loginId"

            every { repository.findByLoginId(loginId) } returnsMany listOf(SampleEntity.user)

            try {
                sut.validateLoginId(loginId)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }
    }

    describe("validateEmail") {
        it("should not throw exception when email is valid") {
            val email = "email"

            every { emailService.isEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returnsMany listOf(null)

            sut.validateEmail(email)
        }

        it("should throw RegisterException when email is not verified") {
            val email = "email"

            every { emailService.isEmailVerified(email) } returns false

            try {
                sut.validateEmail(email)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }

        it("should throw RegisterException when email is duplicate") {
            val email = "email"

            every { emailService.isEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returnsMany listOf(SampleEntity.user)

            try {
                sut.validateEmail(email)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }
    }

    describe("isDuplicateLoginId") {
        it("should return false when loginId is not duplicate") {
            val loginId = "loginId"

            every { repository.findByLoginId(loginId) } returnsMany listOf(null)

            sut.isDuplicateLoginId(loginId) shouldBe false
        }

        it("should return true when loginId is duplicate") {
            val loginId = "loginId"

            every { repository.findByLoginId(loginId) } returnsMany listOf(SampleEntity.user)

            sut.isDuplicateLoginId(loginId) shouldBe true
        }
    }

    describe("checkIsEmailVerified") {
        it("should not throw exception when email is verified") {
            val email = "email"

            every { emailService.isEmailVerified(email) } returns true

            sut.checkIsEmailVerified(email)
        }

        it("should throw RegisterException when email is not verified") {
            val email = "email"

            every { emailService.isEmailVerified(email) } returns false

            try {
                sut.checkIsEmailVerified(email)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }
    }

    describe("checkIsEmailDuplicate") {
        it("should not throw exception when email is not duplicate") {
            val email = "email"

            every { repository.findByEmail(email) } returnsMany listOf(null)

            sut.checkIsEmailDuplicate(email)
        }

        it("should throw RegisterException when email is duplicate") {
            val email = "email"

            every { repository.findByEmail(email) } returnsMany listOf(SampleEntity.user)

            try {
                sut.checkIsEmailDuplicate(email)
            } catch (e: Exception) {
                e shouldBe RegisterException()
            }
        }
    }
})