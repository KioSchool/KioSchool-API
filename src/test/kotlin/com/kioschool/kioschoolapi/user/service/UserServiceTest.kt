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

