package com.kioschool.kioschoolapi.user.facade

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.email.service.SchoolResolver
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import com.kioschool.kioschoolapi.domain.user.service.SessionRefreshResult
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.user.service.UserSessionService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.security.AuthCookieManager
import com.kioschool.kioschoolapi.global.security.JwtProvider
import com.kioschool.kioschoolapi.global.template.TemplateService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import jakarta.servlet.http.Cookie
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration

class UserFacadeTest : DescribeSpec({
    val userService = mockk<UserService>()
    val userSessionService = mockk<UserSessionService>()
    val emailService = mockk<EmailService>()
    val templateService = mockk<TemplateService>()
    val discordService = mockk<DiscordService>()
    val jwtProvider = mockk<JwtProvider>()
    val authCookieManager = AuthCookieManager(true, Duration.ofMinutes(30), Duration.ofDays(7))

    val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    val sut = UserFacade(
        userService,
        userSessionService,
        emailService,
        templateService,
        discordService,
        jwtProvider,
        authCookieManager,
        eventPublisher
    )

    fun MockHttpServletResponse.setCookies() = getHeaders("Set-Cookie")

    beforeTest {
        mockkObject(userService)
        mockkObject(userSessionService)
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
            every { userSessionService.create(user, any()) } returns "1.secret"

            val result = sut.login(loginId, loginPassword, response)

            val cookies = response.setCookies()
            cookies.size shouldBe 2
            cookies[0] shouldStartWith "Authorization=token; Path=/; Max-Age=1800;"
            cookies[1] shouldStartWith "RefreshToken=1.secret; Path=/; Max-Age=604800;"
            assert(result.body == "login success")

            verify { userService.getUser(loginId) }
            verify { userService.checkPassword(user, loginPassword) }
            verify { jwtProvider.createToken(user) }
            verify { userSessionService.create(user, any()) }
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
            verify(exactly = 0) { userSessionService.create(any(), any()) }
        }
    }

    describe("logout") {
        it("should delete this device's session and clear both cookies") {
            val request = MockHttpServletRequest().apply { setCookies(Cookie("RefreshToken", "1.secret")) }
            val response = MockHttpServletResponse()

            every { userSessionService.delete("1.secret", any()) } just Runs

            val result = sut.logout(request, response)

            val cookies = response.setCookies()
            cookies.size shouldBe 2
            cookies[0] shouldStartWith "Authorization=; Path=/; Max-Age=0;"
            cookies[1] shouldStartWith "RefreshToken=; Path=/; Max-Age=0;"
            assert(result.body == "logout success")

            verify { userSessionService.delete("1.secret", any()) }
        }
    }

    describe("refresh") {
        val user = SampleEntity.user

        fun requestWithRefreshToken() =
            MockHttpServletRequest().apply { setCookies(Cookie("RefreshToken", "1.old")) }

        it("should set both cookies when the token is rotated") {
            val response = MockHttpServletResponse()

            every { userSessionService.refresh("1.old", any()) } returns SessionRefreshResult.Rotated(user, "1.new")
            every { jwtProvider.createToken(user) } returns "access"

            val result = sut.refresh(requestWithRefreshToken(), response)

            val cookies = response.setCookies()
            cookies.size shouldBe 2
            cookies[0] shouldStartWith "Authorization=access;"
            cookies[1] shouldStartWith "RefreshToken=1.new;"
            assert(result.body == "refresh success")
        }

        it("should set only the access cookie during the grace period") {
            val response = MockHttpServletResponse()

            every { userSessionService.refresh("1.old", any()) } returns SessionRefreshResult.Grace(user)
            every { jwtProvider.createToken(user) } returns "access"

            sut.refresh(requestWithRefreshToken(), response)

            val cookies = response.setCookies()
            cookies.size shouldBe 1
            cookies[0] shouldStartWith "Authorization=access;"
        }

        it("should clear cookies and throw AUTHENTICATION_REQUIRED when rejected") {
            val response = MockHttpServletResponse()

            every { userSessionService.refresh("1.old", any()) } returns
                SessionRefreshResult.Rejected(SessionRefreshResult.RejectReason.REUSED)

            val ex = assertThrows<CustomException> {
                sut.refresh(requestWithRefreshToken(), response)
            }
            assertEquals(ErrorCode.AUTHENTICATION_REQUIRED, ex.errorCode)

            val cookies = response.setCookies()
            cookies[0] shouldStartWith "Authorization=; Path=/; Max-Age=0;"
            cookies[1] shouldStartWith "RefreshToken=; Path=/; Max-Age=0;"
            verify(exactly = 0) { jwtProvider.createToken(any()) }
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
            every { userSessionService.create(any(), any()) } returns "1.secret"

            val result = sut.register(response, loginId, loginPassword, name, email)

            val cookies = response.setCookies()
            cookies[0] shouldStartWith "Authorization=token; Path=/; Max-Age=1800;"
            cookies[1] shouldStartWith "RefreshToken=1.secret; Path=/; Max-Age=604800;"
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

    describe("isAcquisitionSurveyAnswered") {
        val username = "test"
        val user = SampleEntity.user

        it("should return false when the user has never been asked") {
            every { userService.getUser(username) } returns user
            every { userService.hasAcquisitionSurvey(user) } returns false

            sut.isAcquisitionSurveyAnswered(username) shouldBe false

            verify { userService.getUser(username) }
            verify { userService.hasAcquisitionSurvey(user) }
        }

        it("should return true when a survey row exists") {
            every { userService.getUser(username) } returns user
            every { userService.hasAcquisitionSurvey(user) } returns true

            sut.isAcquisitionSurveyAnswered(username) shouldBe true
        }

        it("should throw CustomException(USER_NOT_FOUND) when user does not exist") {
            every { userService.getUser(username) } throws CustomException(ErrorCode.USER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.isAcquisitionSurveyAnswered(username)
            }
            assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)

            verify(exactly = 0) { userService.hasAcquisitionSurvey(any()) }
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
            every { userSessionService.deleteAllOf(user) } just Runs
            every { emailService.deleteResetPasswordCode(code) } just Runs

            sut.resetPassword(code, password)

            verify { emailService.getEmailByCode(code) }
            verify { userService.getUserByEmail(email) }
            verify { userService.savePassword(user, password) }
            verify { userSessionService.deleteAllOf(user) }
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

    describe("withdraw") {
        fun withdrawingUser() = User(
            loginId = "kimop",
            loginPassword = "hash",
            name = "김운영",
            email = "kim@korea.ac.kr",
            role = UserRole.ADMIN,
            members = mutableListOf()
        )

        it("erases the account, logs out every device and clears the auth cookies") {
            val user = withdrawingUser()
            val response = MockHttpServletResponse()

            every { userService.getUser("kimop") } returns user
            every { userService.checkPassword(user, "pw") } just Runs
            every { userSessionService.deleteAllOf(user) } just Runs
            every { emailService.deleteAllEmailCodes("kim@korea.ac.kr") } just Runs
            every { userService.withdraw(user, any()) } returns user
            every { discordService.sendUserWithdraw(user.id, 0) } just Runs

            sut.withdraw("kimop", "pw", response)

            verifyOrder {
                userService.checkPassword(user, "pw")
                userSessionService.deleteAllOf(user)
                emailService.deleteAllEmailCodes("kim@korea.ac.kr")
                userService.withdraw(user, any())
            }
            verify { discordService.sendUserWithdraw(user.id, 0) }
            response.setCookies().forEach { it.contains("Max-Age=0") shouldBe true }
            response.setCookies().size shouldBe 2
        }

        it("does nothing when the password is wrong") {
            val user = withdrawingUser()

            every { userService.getUser("kimop") } returns user
            every { userService.checkPassword(user, "wrong") } throws CustomException(ErrorCode.LOGIN_FAILED)

            val ex = assertThrows<CustomException> { sut.withdraw("kimop", "wrong", MockHttpServletResponse()) }

            ex.errorCode shouldBe ErrorCode.LOGIN_FAILED
            verify(exactly = 0) { userSessionService.deleteAllOf(any()) }
            verify(exactly = 0) { userService.withdraw(any(), any()) }
        }

        // 수동 가입한 고등학생처럼 이메일이 없는 계정도 있다
        it("skips email codes when the account has no email") {
            val user = withdrawingUser().apply { email = null }

            every { userService.getUser("kimop") } returns user
            every { userService.checkPassword(user, "pw") } just Runs
            every { userSessionService.deleteAllOf(user) } just Runs
            every { userService.withdraw(user, any()) } returns user
            every { discordService.sendUserWithdraw(user.id, 0) } just Runs

            sut.withdraw("kimop", "pw", MockHttpServletResponse())

            verify(exactly = 0) { emailService.deleteAllEmailCodes(any()) }
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
        it("should call getAllUsers and expose loginId for super admin") {
            val keyword = "test"
            val page = 0
            val size = 10
            val users = PageImpl(listOf(SampleEntity.user))

            val schoolResolver = SchoolResolver(mapOf("konkuk.ac.kr" to "건국대학교"))
            every { emailService.getSchoolResolver() } returns schoolResolver
            every { userService.getAllUsers(keyword, emptySet(), null, page, size) } returns users

            val result = sut.getAllUsers(keyword, null, page, size)

            assert(result.content.first().id == users.content.first().id)
            assert(result.content.first().loginId == users.content.first().loginId)
            assert(result.content.first().schoolName == schoolResolver.schoolOf(users.content.first().email))

            verify { userService.getAllUsers(keyword, emptySet(), null, page, size) }
        }
    }
})