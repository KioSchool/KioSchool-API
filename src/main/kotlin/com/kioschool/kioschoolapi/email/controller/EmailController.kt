package com.kioschool.kioschoolapi.email.controller

import com.kioschool.kioschoolapi.email.entity.EmailDomain
import com.kioschool.kioschoolapi.email.facade.EmailFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Email Controller")
@RestController
class EmailController(
    private val emailFacade: EmailFacade
) {
    @Operation(summary = "이메일 도메인 조회", description = "키오스쿨에서 사용 가능한 모든 이메일 도메인을 조회합니다.")
    @GetMapping("/email-domains")
    fun getWorkspaces(
        @RequestParam(required = false) name: String?,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Page<EmailDomain> {
        return emailFacade.getAllEmailDomains(name, page, size)
    }
}