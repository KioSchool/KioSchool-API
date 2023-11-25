package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.common.exception.InvalidJwtException
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.user.dto.*
import com.kioschool.kioschoolapi.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.user.exception.RegisterException
import com.kioschool.kioschoolapi.user.service.UserService
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService,
    private val emailService: EmailService
) {
    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequestBody): String {
        return userService.login(body.id, body.password)
    }

    @PostMapping("/register")
    fun register(@RequestBody body: RegisterRequestBody): String {
        return userService.register(body.id, body.password, body.name, body.email)
    }

    @PostMapping("/duplicate")
    fun isDuplicateLoginId(@RequestBody body: IsDuplicateLoginIdRequestBody): Boolean {
        return userService.isDuplicateLoginId(body.id)
    }

    @PostMapping("/email")
    fun sendEmailCode(@RequestBody body: SendEmailCodeRequestBody) {
        return emailService.sendRegisterCodeEmail(body.email)
    }

    @PostMapping("/verify")
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