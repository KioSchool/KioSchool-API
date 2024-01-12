package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.user.dto.*
import com.kioschool.kioschoolapi.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User Controller")
@RestController
class UserController(
    private val userService: UserService,
    private val emailService: EmailService
) {
    @Operation(summary = "로그인", description = "로그인 성공 시 쿠키에 JWT 토큰을 담아 반환합니다.")
    @PostMapping("/login")
    @ResponseBody
    fun login(
        @Valid @RequestBody body: LoginRequestBody,
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

    @Operation(summary = "로그아웃", description = "쿠키에 담긴 JWT 토큰을 삭제합니다.")
    @PostMapping("/logout")
    @ResponseBody
    fun logout(response: HttpServletResponse): ResponseEntity<String> {
        val cookie = ResponseCookie.from("Authorization", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .sameSite("None")
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
        return ResponseEntity.ok().body("logout success")
    }

    @Operation(
        summary = "회원가입",
        description = "이메일 인증이 되어있어야지만 회원가입에 성공합니다.<br>회원가입 성공 시 쿠키에 JWT 토큰을 담아 반환합니다."
    )
    @PostMapping("/register")
    @ResponseBody
    fun register(
        @Valid @RequestBody body: RegisterRequestBody,
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

    @Operation(summary = "ID 중복 체크", description = "중복되는 ID가 있으면 true를 반환합니다.")
    @PostMapping("/user/duplicate")
    fun isDuplicateLoginId(@Valid @RequestBody body: IsDuplicateLoginIdRequestBody): Boolean {
        return userService.isDuplicateLoginId(body.id)
    }

    @Operation(summary = "이메일 인증코드 발송", description = "이메일 인증코드를 발송합니다.")
    @PostMapping("/user/email")
    fun sendEmailCode(@Valid @RequestBody body: SendEmailCodeRequestBody) {
        return emailService.sendRegisterCodeEmail(body.email)
    }

    @Operation(summary = "이메일 인증코드 확인", description = "이메일 인증코드를 확인합니다.")
    @PostMapping("/user/verify")
    fun verifyEmailCode(@Valid @RequestBody body: VerifyEmailCodeRequestBody): Boolean {
        return emailService.verifyRegisterCode(body.email, body.code)
    }
}