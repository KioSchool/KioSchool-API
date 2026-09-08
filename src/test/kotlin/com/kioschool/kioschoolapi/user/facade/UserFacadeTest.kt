package com.kioschool.kioschoolapi.user.facade

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.security.JwtProvider
import com.kioschool.kioschoolapi.global.template.TemplateService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.mock.web.MockHttpServletResponse

class UserFacadeTest : DescribeSpec({
    val isSecure = true
    val userService = mockk<UserService>()
    val emailService = mockk<EmailService>()
    val templateService = mockk<TemplateService>()
    val discordService = mockk<DiscordService>()
    val jwtProvider = mockk<JwtProvider>()

    val sut = UserFacade(
        isSecure,
        userService,
        emailService,
        templateService,
        discordService,
        jwtProvider
    )

    beforeTest {
        mockkObject(userService)
        mockkObject(emailService)
        mockkObject(templateService)
        mockkObject(discordService)
        mockkObject(jwtProvider)
    }

    afterTest {
        clearAllMocks()
    }

    describe("login") {
        it("should return login success") {
            val loginId = "test"
            val loginPassword = "test"
            val response = MockHttpServletResponse()
            val user = SampleEntity.user

            every { userService.getUser(loginId) } returns user
            every { userService.checkPassword(user, loginPassword) } just Runs
            every { jwtProvider.createToken(user) } returns "token"

            val result = sut.login(loginId, loginPassword, response)


            assert(response.headerNames.contains("Set-Cookie"))
            assert(response.getHeaderValue("Set-Cookie") == "Authorization=token; Path=/; Secure; HttpOnly; SameSite=NONE")
            assert(result.body == "login success")


            verify { userService.getUser(loginId) }
            verify { userService.checkPassword(user, loginPassword) }
            verify { jwtProvider.createToken(user) }
        }

        it("should throw exception when user not found") {
            val loginId = "test"
            val loginPassword = "test"
            val response = MockHttpServletResponse()

            every { userService.getUser(loginId) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.login(loginId, loginPassword, response)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(loginId) }
            verify(exactly = 0) { userService.checkPassword(any(), any()) }
            verify(exactly = 0) { jwtProvider.createToken(any()) }
        }

        it("should throw exception when password is incorrect") {
            val loginId = "test"
            val loginPassword = "test"
            val response = MockHttpServletResponse()
            val user = SampleEntity.user

            every { userService.getUser(loginId) } returns user
            every { userService.checkPassword(user, loginPassword) } throws CustomException(ErrorCode.LOGIN_FAILED)

            val ex = assertThrows<CustomException> {
                sut.login(loginId, loginPassword, response)
            }
            assertEquals(ErrorCode.LOGIN_FAILED, ex.errorCode)

            verify { userService.getUser(loginId) }
            verify { userService.checkPassword(user, loginPassword) }
            verify(exactly = 0) { jwtProvider.createToken(any()) }
        }
    }

    describe("logout") {
        it("should return logout success") {
            val response = MockHttpServletResponse()

            val result = sut.logout(response)

            assert(response.headerNames.contains("Set-Cookie"))
            assert(response.getHeaderValue("Set-Cookie") == "Authorization=; Path=/; Secure; HttpOnly; SameSite=NONE")
            assert(result.body == "logout success")
        }
    }

    describe("register") {
        it("should return register success") {
            val loginId = "test"
            val loginPassword = "test"
            val name = "test"
            val email = "test@test.com"
            val response = MockHttpServletResponse()

            every { userService.validateLoginId(loginId) } just Runs
            every { userService.validateEmail(email) } just Runs
            every { emailService.deleteRegisterCode(email) } just Runs
            every {
                userService.saveUser(
                    loginId,
                    loginPassword,
                    name,
                    email
                )
            } returns SampleEntity.user
            every { discordService.sendUserRegister(any()) } just Runs
            every { jwtProvider.createToken(any()) } returns "token"

            val result = sut.register(response, loginId, loginPassword, name, email)

            assert(response.headerNames.contains("Set-Cookie"))
            assert(response.getHeaderValue("Set-Cookie") == "Authorization=token; Path=/; Secure; HttpOnly; SameSite=NONE")
            assert(result.body == "register success")

            verify { userService.validateLoginId(loginId) }
            verify { userService.validateEmail(email) }
            verify { emailService.deleteRegisterCode(email) }
            verify { userService.saveUser(loginId, loginPassword, name, email) }
            verify { discordService.sendUserRegister(any()) }
            verify { jwtProvider.createToken(any()) }
        }

        it("should throw exception when loginId is invalid") {
            val loginId = "test"
            val loginPassword = "test"
            val name = "test"
            val email = "test@test.com"
            val response = MockHttpServletResponse()

            every { userService.validateLoginId(loginId) } throws CustomException(ErrorCode.DUPLICATE_LOGIN_ID)

            val ex = assertThrows<CustomException> {
                sut.register(response, loginId, loginPassword, name, email)
            }
            assertEquals(ErrorCode.DUPLICATE_LOGIN_ID, ex.errorCode)

            verify { userService.validateLoginId(loginId) }
            verify(exactly = 0) { userService.validateEmail(any()) }
            verify(exactly = 0) { emailService.deleteRegisterCode(any()) }
            verify(exactly = 0) { userService.saveUser(any(), any(), any(), any()) }
            verify(exactly = 0) { discordService.sendUserRegister(any()) }
            verify(exactly = 0) { jwtProvider.createToken(any()) }
        }

        it("should throw exception when email is invalid") {
            val loginId = "test"
            val loginPassword = "test"
            val name = "test"
            val email = "test@test.com"
            val response = MockHttpServletResponse()

            every { userService.validateLoginId(loginId) } just Runs
            every { userService.validateEmail(email) } throws CustomException(ErrorCode.DUPLICATE_EMAIL)

            val ex = assertThrows<CustomException> {
                sut.register(response, loginId, loginPassword, name, email)
            }
            assertEquals(ErrorCode.DUPLICATE_EMAIL, ex.errorCode)

            verify { userService.validateLoginId(loginId) }
            verify { userService.validateEmail(email) }
            verify(exactly = 0) { emailService.deleteRegisterCode(any()) }
            verify(exactly = 0) { userService.saveUser(any(), any(), any(), any()) }
            verify(exactly = 0) { discordService.sendUserRegister(any()) }
            verify(exactly = 0) { jwtProvider.createToken(any()) }
        }
    }

    describe("saveAcquisitionSurvey") {
        val username = "test"
        val user = SampleEntity.user

        // 정규화 책임이 파사드로 옮겨왔다. userService에 실제로 넘어가는 값으로 규칙을 고정한다.
        fun saveAndCapture(
            channel: AcquisitionChannel?,
            channelEtc: String?,
            context: String?
        ): Triple<AcquisitionChannel?, String?, String?> {
            var captured: Triple<AcquisitionChannel?, String?, String?>? = null

            every { userService.getUser(username) } returns user
            every {
                userService.saveAcquisitionSurvey(user, any(), any(), any())
            } answers {
                captured = Triple(arg<AcquisitionChannel?>(1), arg<String?>(2), arg<String?>(3))
                mockk<AcquisitionSurvey>()
            }

            sut.saveAcquisitionSurvey(username, channel, channelEtc, context)

            verify { userService.getUser(username) }
            return captured!!
        }

        it("should drop channelEtc when channel is not ETC") {
            val (channel, channelEtc, _) =
                saveAndCapture(AcquisitionChannel.INSTAGRAM, "\uce5c\uad6c\uac00 \uc54c\ub824\uc90c", null)

            channel shouldBe AcquisitionChannel.INSTAGRAM
            channelEtc shouldBe null
        }

        it("should keep channelEtc when channel is ETC") {
            val (_, channelEtc, _) = saveAndCapture(AcquisitionChannel.ETC, "\uad50\uc218\ub2d8 \ucd94\ucc9c", null)

            channelEtc shouldBe "\uad50\uc218\ub2d8 \ucd94\ucc9c"
        }

        it("should allow ETC without channelEtc") {
            val (channel, channelEtc, _) = saveAndCapture(AcquisitionChannel.ETC, null, null)

            channel shouldBe AcquisitionChannel.ETC
            channelEtc shouldBe null
        }

        it("should convert blank channelEtc to null") {
            val (_, channelEtc, _) = saveAndCapture(AcquisitionChannel.ETC, "   ", null)

            channelEtc shouldBe null
        }

        it("should keep context regardless of channel") {
            val (channel, _, context) = saveAndCapture(null, null, "source=instagram")

            channel shouldBe null
            context shouldBe "source=instagram"
        }

        it("should convert blank context to null") {
            val (_, _, context) = saveAndCapture(AcquisitionChannel.INSTAGRAM, null, "   ")

            context shouldBe null
        }

        it("should throw CustomException(USER_NOT_FOUND) when user does not exist") {
            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.saveAcquisitionSurvey(username, null, null, null)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(username) }
            verify(exactly = 0) { userService.saveAcquisitionSurvey(any(), any(), any(), any()) }
        }
    }

    describe("isDuplicateLoginId") {
        it("should return true when loginId is duplicate") {
            val loginId = "test"

            every { userService.isDuplicateLoginId(loginId) } returns true

            val result = sut.isDuplicateLoginId(loginId)

            assert(result)

            verify { userService.isDuplicateLoginId(loginId) }
        }

        it("should return false when loginId is not duplicate") {
            val loginId = "test"

            every { userService.isDuplicateLoginId(loginId) } returns false

            val result = sut.isDuplicateLoginId(loginId)

            assert(!result)

            verify { userService.isDuplicateLoginId(loginId) }
        }
    }

    describe("sendRegisterEmail") {
        it("should send register email") {
            val email = "test@test.com"
            val testTemplate = "test"
            val code = "123456"

            every { emailService.validateEmailDomainVerified(email) } just Runs
            every { emailService.generateRegisterCode() } returns code
            every { templateService.getRegisterEmailTemplate(code) } returns testTemplate
            every { emailService.sendEmail(email, any(), testTemplate) } just Runs
            every {
                emailService.createOrUpdateRegisterEmailCode(
                    email,
                    code
                )
            } returns SampleEntity.emailCode

            sut.sendRegisterEmail(email)

            verify { emailService.validateEmailDomainVerified(email) }
            verify { emailService.generateRegisterCode() }
            verify { templateService.getRegisterEmailTemplate(code) }
            verify { emailService.sendEmail(email, "키오스쿨 회원가입 인증 코드", testTemplate) }
            verify { emailService.createOrUpdateRegisterEmailCode(email, code) }
        }

        it("should throw CustomException when email domain is not verified") {
            val email = "test@test.com"

            every { emailService.validateEmailDomainVerified(email) } throws CustomException(ErrorCode.NOT_VERIFIED_EMAIL_DOMAIN)

            val ex = assertThrows<CustomException> {
                sut.sendRegisterEmail(email)
            }
            assertEquals(ErrorCode.NOT_VERIFIED_EMAIL_DOMAIN, ex.errorCode)

            verify { emailService.validateEmailDomainVerified(email) }
            verify(exactly = 0) { emailService.generateRegisterCode() }
            verify(exactly = 0) { templateService.getRegisterEmailTemplate(any()) }
            verify(exactly = 0) { emailService.sendEmail(any(), any(), any()) }
            verify(exactly = 0) { emailService.createOrUpdateRegisterEmailCode(any(), any()) }
        }
    }

    describe("verifyRegisterCode") {
        it("should return true when email code is verified") {
            val email = "test@test.com"
            val code = "123456"

            every { emailService.verifyRegisterCode(email, code) } returns true

            val result = sut.verifyRegisterCode(email, code)

            assert(result)

            verify { emailService.verifyRegisterCode(email, code) }
        }

        it("should return false when email code is not verified") {
            val email = "test@test.com"
            val code = "123456"

            every { emailService.verifyRegisterCode(email, code) } returns false

            val result = sut.verifyRegisterCode(email, code)

            assert(!result)

            verify { emailService.verifyRegisterCode(email, code) }
        }
    }


    describe("sendResetPasswordEmail") {
        it("should send reset password email") {
            val loginId = "test"
            val email = "test@test.com"
            val user = SampleEntity.user
            val code = "123456"
            val testTemplate = "test"

            every { userService.getUser(loginId) } returns user
            every { userService.checkEmailAddress(user, email) } just Runs
            every { emailService.generateResetPasswordCode() } returns code
            every { templateService.getResetPasswordEmailTemplate(code) } returns testTemplate
            every { emailService.sendEmail(email, any(), testTemplate) } just Runs
            every {
                emailService.createOrUpdateResetPasswordEmailCode(
                    email,
                    code
                )
            } returns SampleEntity.emailCode

            sut.sendResetPasswordEmail(loginId, email)

            verify { userService.getUser(loginId) }
            verify { userService.checkEmailAddress(user, email) }
            verify { emailService.generateResetPasswordCode() }
            verify { templateService.getResetPasswordEmailTemplate(code) }
            verify { emailService.sendEmail(email, "키오스쿨 비밀번호 재설정", testTemplate) }
            verify { emailService.createOrUpdateResetPasswordEmailCode(email, code) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val loginId = "test"
            val email = "different@email.com"

            every { userService.getUser(loginId) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.sendResetPasswordEmail(loginId, email)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(loginId) }
            verify(exactly = 0) { userService.checkEmailAddress(any(), any()) }
            verify(exactly = 0) { emailService.generateResetPasswordCode() }
            verify(exactly = 0) { templateService.getResetPasswordEmailTemplate(any()) }
            verify(exactly = 0) { emailService.sendEmail(any(), any(), any()) }
            verify(exactly = 0) { emailService.createOrUpdateResetPasswordEmailCode(any(), any()) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when email is different") {
            val loginId = "test"
            val email = "different@email.com"
            val user = SampleEntity.user

            every { userService.getUser(loginId) } returns user
            every { userService.checkEmailAddress(user, email) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.sendResetPasswordEmail(loginId, email)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(loginId) }
            verify { userService.checkEmailAddress(user, email) }
            verify(exactly = 0) { emailService.generateResetPasswordCode() }
            verify(exactly = 0) { templateService.getResetPasswordEmailTemplate(any()) }
            verify(exactly = 0) { emailService.sendEmail(any(), any(), any()) }
            verify(exactly = 0) { emailService.createOrUpdateResetPasswordEmailCode(any(), any()) }
        }
    }

    describe("resetPassword") {
        it("should reset password") {
            val code = "123456"
            val password = "test"
            val email = "test@test.com"
            val user = SampleEntity.user

            every { emailService.getEmailByCode(code) } returns email
            every { userService.getUserByEmail(email) } returns user
            every { userService.savePassword(user, password) } returns user
            every { emailService.deleteResetPasswordCode(code) } just Runs

            sut.resetPassword(code, password)

            verify { emailService.getEmailByCode(code) }
            verify { userService.getUserByEmail(email) }
            verify { userService.savePassword(user, password) }
            verify { emailService.deleteResetPasswordCode(code) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when email is not found") {
            val code = "123456"
            val password = "test"

            every { emailService.getEmailByCode(code) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.resetPassword(code, password)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { emailService.getEmailByCode(code) }
            verify(exactly = 0) { userService.getUserByEmail(any()) }
            verify(exactly = 0) { userService.savePassword(any(), any()) }
            verify(exactly = 0) { emailService.deleteResetPasswordCode(any()) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user is not found") {
            val code = "123456"
            val password = "test"
            val email = "test@test.com"

            every { emailService.getEmailByCode(code) } returns email
            every { userService.getUserByEmail(email) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.resetPassword(code, password)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { emailService.getEmailByCode(code) }
            verify { userService.getUserByEmail(email) }
            verify(exactly = 0) { userService.savePassword(any(), any()) }
            verify(exactly = 0) { emailService.deleteResetPasswordCode(any()) }
        }
    }

    describe("getUser") {
        it("should return user") {
            val loginId = "test"
            val user = SampleEntity.user

            every { userService.getUser(loginId) } returns user

            val result = sut.getUser(loginId)

            assert(result.id == user.id)

            verify { userService.getUser(loginId) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val loginId = "test"

            every { userService.getUser(loginId) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.getUser(loginId)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(loginId) }
        }
    }

    describe("deleteUser") {
        it("should delete user") {
            val loginId = "test"
            val user = SampleEntity.user

            every { userService.getUser(loginId) } returns user
            every { userService.deleteUser(user) } returns user

            val result = sut.deleteUser(loginId)

            assert(result.id == user.id)

            verify { userService.getUser(loginId) }
            verify { userService.deleteUser(user) }
        }

        it("should throw CustomException(USER_NOT_FOUND) when user not found") {
            val loginId = "test"

            every { userService.getUser(loginId) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.deleteUser(loginId)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify { userService.getUser(loginId) }
            verify(exactly = 0) { userService.deleteUser(any()) }
        }
    }

    describe("createSuperAdminUser") {
        it("should create super admin user") {
            val username = "test"
            val id = "test"
            val superAdminUser = SampleEntity.user
            val user = SampleEntity.user

            every { userService.getUser(username) } returns superAdminUser
            every { userService.checkHasSuperAdminPermission(superAdminUser) } just Runs
            every { userService.getUser(id) } returns user
            every { userService.saveUser(user) } returns user

            val result = sut.createSuperAdminUser(username, id)

            assert(result.id == user.id)
            assert(result.role == UserRole.SUPER_ADMIN)

            verify { userService.getUser(username) }
            verify { userService.checkHasSuperAdminPermission(superAdminUser) }
            verify { userService.getUser(id) }
            verify { userService.saveUser(user) }
        }

        it("should throw CustomException(NO_PERMISSION) when user is not super admin") {
            val username = "super admin username"
            val id = "admin username"
            val user = SampleEntity.user

            every { userService.getUser(username) } returns user
            every { userService.checkHasSuperAdminPermission(user) } throws CustomException(ErrorCode.NO_PERMISSION)

            val ex = assertThrows<CustomException> {
                sut.createSuperAdminUser(username, id)
            }
            assertEquals(ErrorCode.NO_PERMISSION, ex.errorCode)

            verify { userService.getUser(username) }
            verify { userService.checkHasSuperAdminPermission(user) }
            verify(exactly = 0) { userService.getUser(id) }
            verify(exactly = 0) { userService.saveUser(any()) }
        }
    }

    describe("registerAccountUrl") {
        it("should register account url") {
            val username = "test"
            val accountUrl = "testUrl"
            val user = SampleEntity.user

            every { userService.getUser(username) } returns user
            every { userService.removeAmountQueryFromAccountUrl(accountUrl) } returns accountUrl
            every { userService.saveUser(user) } returns user

            val result = sut.registerAccountUrl(username, accountUrl)

            assert(result.id == user.id)
            assert(result.accountUrl == accountUrl)

            verify { userService.getUser(username) }
            verify { userService.removeAmountQueryFromAccountUrl(accountUrl) }
            verify { userService.saveUser(user) }
        }
    }

    describe("getAllUsers") {
        it("should call getAllUsers") {
            val name = "test"
            val page = 0
            val size = 10
            val users = PageImpl(listOf(SampleEntity.user))

            every { userService.getAllUsers(name, page, size) } returns users

            val result = sut.getAllUsers(name, page, size)

            assert(result.content.first().id == users.content.first().id)

            verify { userService.getAllUsers(name, page, size) }
        }
    }
})