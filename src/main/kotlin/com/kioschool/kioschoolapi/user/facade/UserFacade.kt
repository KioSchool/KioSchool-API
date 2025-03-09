package com.kioschool.kioschoolapi.user.facade

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.discord.service.DiscordService
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.template.TemplateService
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.service.UserService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.server.Cookie.SameSite
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val discordService: DiscordService,
    private val jwtProvider: JwtProvider
) {
    fun login(
        loginId: String,
        loginPassword: String,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        val user = userService.getUser(loginId)
        userService.checkPassword(user, loginPassword)

        val token = jwtProvider.createToken(user)
        val authCookie =
            ResponseCookie.from(HttpHeaders.AUTHORIZATION, token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite(SameSite.NONE.name)
                .build()

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString())
        return ResponseEntity.ok().body("login success")
    }

    fun logout(response: HttpServletResponse): ResponseEntity<String> {
        val authCookie = ResponseCookie.from(HttpHeaders.AUTHORIZATION, "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite(SameSite.NONE.name)
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString())
        return ResponseEntity.ok().body("logout success")
    }

    fun register(
        response: HttpServletResponse,
        loginId: String,
        loginPassword: String,
        name: String,
        email: String
    ): ResponseEntity<String> {
        userService.validateLoginId(loginId)
        userService.validateEmail(email)
        emailService.deleteRegisterCode(email)

        val user = userService.saveUser(loginId, loginPassword, name, email)

        discordService.sendUserRegister(user)

        val token = jwtProvider.createToken(user)
        val cookie = ResponseCookie.from(HttpHeaders.AUTHORIZATION, token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite(SameSite.NONE.name)
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        return ResponseEntity.ok().body("register success")
    }

    fun isDuplicateLoginId(loginId: String): Boolean {
        return userService.isDuplicateLoginId(loginId)
    }

    fun sendRegisterEmail(emailAddress: String) {
        emailService.validateEmailDomainVerified(emailAddress)

        val code = emailService.generateRegisterCode()
        emailService.sendEmail(
            emailAddress,
            "키오스쿨 회원가입 인증 코드",
            templateService.getRegisterEmailTemplate(code)
        )

        emailService.createOrUpdateRegisterEmailCode(emailAddress, code)
    }

    fun verifyRegisterCode(email: String, code: String): Boolean {
        return emailService.verifyRegisterCode(email, code)
    }

    fun sendResetPasswordEmail(loginId: String, email: String) {
        val user = userService.getUser(loginId)
        userService.checkEmailAddress(user, email)

        val code = emailService.generateResetPasswordCode()
        emailService.sendEmail(
            email,
            "키오스쿨 비밀번호 재설정",
            templateService.getResetPasswordEmailTemplate(code)
        )

        emailService.createOrUpdateResetPasswordEmailCode(email, code)
    }

    fun resetPassword(code: String, password: String) {
        val email = emailService.getEmailByCode(code)
        val user = userService.getUserByEmail(email)
        userService.savePassword(user, password)
        emailService.deleteResetPasswordCode(code)
    }

    fun getUser(loginId: String) = userService.getUser(loginId)

    fun deleteUser(loginId: String): User {
        val user = userService.getUser(loginId)
        return userService.deleteUser(user)
    }

    fun createSuperAdminUser(username: String, id: String): User {
        val superAdminUser = userService.getUser(username)
        userService.checkHasSuperAdminPermission(superAdminUser)

        val user = userService.getUser(id)
        user.role = UserRole.SUPER_ADMIN
        return userService.saveUser(user)
    }

    fun registerAccountUrl(username: String, accountUrl: String): User {
        val user = userService.getUser(username)
        user.accountUrl = userService.removeAmountQueryFromAccountUrl(accountUrl)

        return userService.saveUser(user)
    }

    fun getAllUsers(name: String?, page: Int, size: Int) = userService.getAllUsers(name, page, size)
}