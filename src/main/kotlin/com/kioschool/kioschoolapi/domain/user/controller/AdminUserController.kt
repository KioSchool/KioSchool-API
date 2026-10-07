package com.kioschool.kioschoolapi.domain.user.controller

import com.kioschool.kioschoolapi.domain.user.dto.admin.AcquisitionRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.admin.AcquisitionSurveyStatusResponse
import com.kioschool.kioschoolapi.domain.user.dto.admin.CreateSuperUserRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.admin.RegisterAccountUrlRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.admin.WithdrawRequestBody
import com.kioschool.kioschoolapi.domain.user.dto.common.UserDto
import com.kioschool.kioschoolapi.domain.user.facade.UserFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin User Controller")
@RestController
@RequestMapping("/admin")
class AdminUserController(
    private val userFacade: UserFacade
) {
    @Operation(summary = "로그인 유저 정보 조회", description = "현재 로그인한 유저의 정보를 조회합니다.")
    @GetMapping("/user")
    fun getUser(@AdminUsername username: String): UserDto {
        return userFacade.getUser(username)
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "비밀번호를 확인한 뒤 계정의 개인정보를 지우거나 익명값으로 바꾸고 인증 쿠키를 삭제합니다.<br>운영한 주점과 주문 기록은 매출 통계로 남습니다.<br>비밀번호가 틀리면 401(LOGIN_FAILED)을 반환합니다."
    )
    @PostMapping("/user/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(
        @AdminUsername username: String,
        @Valid @RequestBody body: WithdrawRequestBody,
        response: HttpServletResponse
    ) {
        userFacade.withdraw(username, body.password, response)
    }

    @Operation(summary = "슈퍼 유저 생성", description = "슈퍼 유저를 생성합니다.<br>슈퍼 유저는 슈퍼 유저만 지정할 수 있습니다.")
    @PostMapping("/super-user")
    fun createSuperUser(
        @AdminUsername username: String,
        @RequestBody body: CreateSuperUserRequestBody
    ): UserDto {
        return userFacade.createSuperAdminUser(username, body.id)
    }

    @Operation(summary = "토스 계좌 URL 등록", description = "토스 계좌 URL을 등록합니다.")
    @PostMapping("/user/toss-account")
    fun registerAccountUrl(
        @AdminUsername username: String,
        @RequestBody body: RegisterAccountUrlRequestBody
    ): UserDto {
        return userFacade.registerAccountUrl(username, body.accountUrl)
    }

    @Operation(
        summary = "유입 경로 설문 응답 여부 조회",
        description = "설문을 이미 물어봤는지 반환합니다.<br>건너뛴 경우도 응답한 것으로 봅니다."
    )
    @GetMapping("/user/acquisition")
    fun getAcquisitionSurveyStatus(@AdminUsername username: String): AcquisitionSurveyStatusResponse {
        return AcquisitionSurveyStatusResponse(userFacade.isAcquisitionSurveyAnswered(username))
    }

    @Operation(
        summary = "유입 경로 등록",
        description = "회원가입 직후 설문한 유입 경로를 저장합니다.<br>이미 저장된 값이 있으면 덮어씁니다."
    )
    @PostMapping("/user/acquisition")
    fun saveAcquisitionSurvey(
        @AdminUsername username: String,
        @Valid @RequestBody body: AcquisitionRequestBody
    ) {
        userFacade.saveAcquisitionSurvey(username, body.channel, body.channelEtc, body.context)
    }
}