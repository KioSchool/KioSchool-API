package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.common.exception.InvalidJwtException
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.user.dto.*
import com.kioschool.kioschoolapi.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.user.exception.RegisterException
import com.kioschool.kioschoolapi.user.service.UserService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class UserController(
    private val userService: UserService,
    private val emailService: EmailService
) {
    @PostMapping("/login")
    @ResponseBody
    fun login(
        @RequestBody body: LoginRequestBody,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        val cookie = ResponseCookie.from("Authorization", userService.login(body.id, body.password))
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        return ResponseEntity.ok().body("login success")
    }

    @PostMapping("/register")
    @ResponseBody
    fun register(
        @RequestBody body: RegisterRequestBody,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        val cookie = ResponseCookie.from(
            "Authorization",
            userService.register(body.id, body.password, body.name, body.email)
        )
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        return ResponseEntity.ok().body("register success")
    }

    @PostMapping("/user/duplicate")
    fun isDuplicateLoginId(@RequestBody body: IsDuplicateLoginIdRequestBody): Boolean {
        return userService.isDuplicateLoginId(body.id)
    }

    @PostMapping("/user/email")
    fun sendEmailCode(@RequestBody body: SendEmailCodeRequestBody) {
        return emailService.sendRegisterCodeEmail(body.email)
    }

    @PostMapping("/user/verify")
    fun verifyEmailCode(@RequestBody body: VerifyEmailCodeRequestBody): Boolean {
        return emailService.verifyRegisterCode(body.email, body.code)
    }

    @ExceptionHandler(
        InvalidJwtException::class,
        LoginFailedException::class,
        RegisterException::class
    )
    fun handle(e: Exception): ExceptionResponseBody {
        return ExceptionResponseBody(e.message ?: "알 수 없는 오류가 발생했습니다.")
    }
}