package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.user.exception.NoPermissionException
import com.kioschool.kioschoolapi.user.exception.RegisterException
import com.kioschool.kioschoolapi.user.exception.UserNotFoundException
import com.kioschool.kioschoolapi.user.repository.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest : DescribeSpec({
    val repository = mockk<UserRepository>()
    val emailService = mockk<EmailService>()
    val jwtProvider = mockk<JwtProvider>()
    val passwordEncoder = BCryptPasswordEncoder()
    val mockPasswordEncoder = mockk<PasswordEncoder>()
    val discordService = mockk<DiscordService>()
    val mockService = mockk<UserService>()
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

    describe("isDuplicateEmail") {
        it("should return false when email is not duplicate") {
            val email = "email"

            every { repository.findByEmail(email) } returnsMany listOf(null)

            sut.isDuplicateEmail(email) shouldBe false
        }

        it("should return true when email is duplicate") {
            val email = "email"

            every { repository.findByEmail(email) } returnsMany listOf(SampleEntity.user)

            sut.isDuplicateEmail(email) shouldBe true
        }
    }

    describe("getUser") {
        it("should return user when user exists") {
            val loginId = "loginId"
            val user = SampleEntity.user

            every { repository.findByLoginId(loginId) } returns user

            sut.getUser(loginId) shouldBe user
        }

        it("should throw UserNotFoundException when user doesn't exist") {
            val loginId = "loginId"

            every { repository.findByLoginId(loginId) } returns null

            try {
                sut.getUser(loginId)
            } catch (e: Exception) {
                e shouldBe UserNotFoundException()
            }
        }
    }

    describe("getAllUsers") {
        it("should return all users") {
            val page = 0
            val size = 10
            val pageRequest = PageRequest.of(page, size)

            every { repository.findAll(pageRequest) } returns PageImpl(listOf(SampleEntity.user))

            sut.getAllUsers(page, size) shouldBe PageImpl(listOf(SampleEntity.user))
        }
    }

    describe("isSuperAdminUser") {
        it("should return true when user is super admin") {
            val username = "username"
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            every { repository.findByLoginId(username) } returns user

            sut.isSuperAdminUser(username) shouldBe true
        }

        it("should return false when user is not super admin") {
            val username = "username"
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            every { repository.findByLoginId(username) } returns user

            sut.isSuperAdminUser(username) shouldBe false
        }
    }

    describe("createSuperAdminUser") {
        it("should return user when user is super admin") {
            val username = "username"
            val id = "id"
            val superAdminUser = SampleEntity.user
            superAdminUser.role = UserRole.SUPER_ADMIN

            every { repository.findByLoginId(username) } returns superAdminUser
            every { repository.findByLoginId(id) } returns User(
                loginId = "test",
                loginPassword = "test",
                name = "test",
                email = "test",
                role = UserRole.ADMIN,
                members = mutableListOf()
            )
            every { repository.save(any()) } returns SampleEntity.user

            sut.createSuperAdminUser(username, id).role shouldBe UserRole.SUPER_ADMIN
        }

        it("should throw NoPermissionException when user is not super admin") {
            val username = "username"
            val id = "id"
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            every { repository.findByLoginId(username) } returns user

            try {
                sut.createSuperAdminUser(username, id)
            } catch (e: Exception) {
                e shouldBe NoPermissionException()
            }
        }
    }

    describe("checkHasSuperAdminPermission") {
        it("should not throw exception when user is super admin") {
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            sut.checkHasSuperAdminPermission(user)
        }

        it("should throw NoPermissionException when user is not super admin") {
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            try {
                sut.checkHasSuperAdminPermission(user)
            } catch (e: Exception) {
                e shouldBe NoPermissionException()
            }
        }
    }

    describe("registerAccountUrl") {
        it("should return user when register account url success") {
            val user = SampleEntity.user
            val username = user.loginId
            val accountUrl = "accountUrl"

            every { repository.findByLoginId(username) } returns user
            every { repository.save(user) } returns user
            every { mockService.removeAmountQueryFromAccountUrl(accountUrl) } returnsMany listOf(
                accountUrl
            )

            sut.registerAccountUrl(username, accountUrl).accountUrl shouldBe accountUrl
        }
    }

    describe("removeAmountQueryFromAccountUrl") {
        it("should return account url without amount query") {
            val accountUrl = "accountUrl?amount=1000&accountNo=1234"

            sut.removeAmountQueryFromAccountUrl(accountUrl) shouldBe "accountUrl?accountNo=1234"
        }
    }

    describe("sendResetPasswordEmail") {
        it("should send reset password email") {
            val loginId = "loginId"
            val user = SampleEntity.user
            val email = user.email

            every { repository.findByLoginId(loginId) } returns user
            every { emailService.sendResetPasswordEmail(email) } returns Unit

            sut.sendResetPasswordEmail(loginId, email)
        }

        it("should throw UserNotFoundException when email is different") {
            val loginId = "loginId"
            val user = SampleEntity.user
            val email = "differentEmail"

            every { repository.findByLoginId(loginId) } returns user

            try {
                sut.sendResetPasswordEmail(loginId, email)
            } catch (e: Exception) {
                e shouldBe UserNotFoundException()
            }
        }

        it("should throw UserNotFoundException when user doesn't exist") {
            val loginId = "loginId"
            val email = "email"

            every { repository.findByLoginId(loginId) } returns null

            try {
                sut.sendResetPasswordEmail(loginId, email)
            } catch (e: Exception) {
                e shouldBe UserNotFoundException()
            }
        }
    }
})