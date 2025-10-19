package com.kioschool.kioschoolapi.domain.user.controller

import com.kioschool.kioschoolapi.domain.user.dto.request.IsDuplicateLoginIdRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.request.LoginRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.request.RegisterRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.request.ResetPasswordRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.request.SendEmailCodeRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.request.SendResetPasswordEmailRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.request.VerifyEmailCodeRequestBody
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User Controller")
@RestController
class UserController(
    private val userFacade: UserFacade
) {
    @Operation(summary = "로그인", description = "로그인 성공 시 쿠키에 JWT 토큰을 담아 반환합니다.")
    @PostMapping("/login")
    @ResponseBody
    fun login(
        @Valid @RequestBody body: LoginRequestBody,
        response: HttpServletResponse
    ): ResponseEntity<String> {
        return userFacade.login(body.id, body.password, response)
    }

    @Operation(summary = "로그아웃", description = "쿠키에 담긴 JWT 토큰을 삭제합니다.")
    @PostMapping("/logout")
    @ResponseBody
    fun logout(response: HttpServletResponse): ResponseEntity<String> {
        return userFacade.logout(response)
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
        return userFacade.register(response, body.id, body.password, body.name, body.email)
    }

    @Operation(summary = "ID 중복 체크", description = "중복되는 ID가 있으면 true를 반환합니다.")
    @PostMapping("/user/duplicate")
    fun isDuplicateLoginId(@Valid @RequestBody body: IsDuplicateLoginIdRequestBody): Boolean {
        return userFacade.isDuplicateLoginId(body.id)
    }

    @Operation(summary = "회원가입 이메일 인증코드 발송", description = "회원가입 이메일 인증코드를 발송합니다.")
    @PostMapping("/user/email")
    fun sendRegisterEmail(@Valid @RequestBody body: SendEmailCodeRequestBody) {
        return userFacade.sendRegisterEmail(body.email)
    }

    @Operation(summary = "회원가입 이메일 인증코드 확인", description = "회원가입 이메일 인증코드를 확인합니다.")
    @PostMapping("/user/verify")
    fun verifyEmailCode(@Valid @RequestBody body: VerifyEmailCodeRequestBody): Boolean {
        return userFacade.verifyRegisterCode(body.email, body.code)
    }

    @Operation(summary = "비밀번호 재설정 이메일 전송", description = "비밀번호 재설정 이메일을 전송합니다.")
    @PostMapping("/user/password")
    fun sendResetPasswordEmail(@Valid @RequestBody body: SendResetPasswordEmailRequestBody) {
        return userFacade.sendResetPasswordEmail(body.id, body.email)
    }

    @Operation(summary = "비밀번호 재설정", description = "비밀번호를 재설정합니다.")
    @PostMapping("/user/reset")
    fun resetPassword(@Valid @RequestBody body: ResetPasswordRequestBody) {
        return userFacade.resetPassword(body.code, body.password)
    }
}