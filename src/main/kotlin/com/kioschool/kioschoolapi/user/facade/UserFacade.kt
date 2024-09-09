package com.kioschool.kioschoolapi.user.facade

import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.exception.UserNotFoundException
import com.kioschool.kioschoolapi.user.service.UserService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class UserFacade(
    private val userService: UserService,
    private val emailService: EmailService,
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
            ResponseCookie.from("Authorization", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("None")
                .build()

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString())
        return ResponseEntity.ok().body("login success")
    }

    fun logout(response: HttpServletResponse): ResponseEntity<String> {
        val authCookie = ResponseCookie.from("Authorization", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
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
        val cookie = ResponseCookie.from("Authorization", token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        return ResponseEntity.ok().body("register success")
    }

    fun isDuplicateLoginId(loginId: String): Boolean {
        return userService.isDuplicateLoginId(loginId)
    }

    fun sendEmailCode(email: String) {
        emailService.sendRegisterCodeEmail(email)
    }

    fun verifyEmailCode(email: String, code: String): Boolean {
        return emailService.verifyRegisterCode(email, code)
    }

    fun sendResetPasswordEmail(loginId: String, email: String) {
        val user = userService.getUser(loginId)
        userService.checkEmailAddress(user, email)

        emailService.sendResetPasswordEmail(email)
    }

    fun resetPassword(code: String, password: String) {
        val email = emailService.getEmailByCode(code) ?: throw UserNotFoundException()
        val user = userService.getUserByEmail(email)
        userService.savePassword(user, password)
        emailService.deleteResetPasswordCode(code)
    }
}