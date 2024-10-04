package com.kioschool.kioschoolapi.email.controller

import com.kioschool.kioschoolapi.common.annotation.SuperAdmin
import com.kioschool.kioschoolapi.email.dto.RegisterEmailDomainRequestBody
import com.kioschool.kioschoolapi.email.dto.RemoveEmailDomainRequestBody
import com.kioschool.kioschoolapi.email.entity.EmailDomain
import com.kioschool.kioschoolapi.email.service.EmailService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Email Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminEmailController(
    private val emailService: EmailService,
) {
    @Operation(summary = "이메일 도메인 조회", description = "키오스쿨에서 사용 가능한 모든 이메일 도메인을 조회합니다.")
    @GetMapping("/email-domains")
    fun getWorkspaces(
        @SuperAdmin username: String,
        @RequestParam(required = false) name: String?,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Page<EmailDomain> {
        return emailService.getAllEmailDomains(name, page, size)
    }

    @Operation(summary = "이메일 도메인 등록", description = "키오스쿨에서 사용 가능하게끔 이메일 도메인을 등록합니다.")
    @PostMapping("/email-domain")
    fun registerEmailDomain(
        @SuperAdmin username: String,
        @RequestBody body: RegisterEmailDomainRequestBody
    ): EmailDomain {
        return emailService.registerEmailDomain(body.name, body.domain)
    }

    @Operation(summary = "이메일 도메인 삭제", description = "키오스쿨에 등록된 이메일 도메인을 삭제합니다.")
    @DeleteMapping("/email-domain")
    fun removeEmailDomain(
        @SuperAdmin username: String,
        @RequestBody body: RemoveEmailDomainRequestBody
    ): EmailDomain {
        return emailService.removeEmailDomain(body.domainId)
    }
}