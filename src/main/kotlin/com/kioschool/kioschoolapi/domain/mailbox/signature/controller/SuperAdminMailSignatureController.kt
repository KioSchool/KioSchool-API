package com.kioschool.kioschoolapi.domain.mailbox.signature.controller

import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailPreviewDto
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSettingsDto
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.signature.facade.MailSignatureFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Mail Signature Controller")
@RestController
@RequestMapping("/super-admin/mail/settings")
class SuperAdminMailSignatureController(
    private val mailSignatureFacade: MailSignatureFacade,
) {
    @Operation(summary = "인사말·서명 조회", description = "내 설정과 팀 공용 설정, 편집기에 보여 줄 서명 HTML을 함께 돌려줍니다.")
    @GetMapping
    fun getSettings(@AdminUsername username: String): MailSettingsDto {
        return mailSignatureFacade.getSettings(username)
    }

    @Operation(summary = "내 인사말·서명 저장", description = "새 메일과 답장에 들어갑니다.")
    @PutMapping("/me")
    fun saveMine(
        @AdminUsername username: String,
        @RequestBody @Valid body: MailSignatureRequestBody,
    ): MailSettingsDto {
        return mailSignatureFacade.saveMine(username, body)
    }

    @Operation(summary = "팀 공용 인사말·서명 저장", description = "여러 곳 보내기와 내 설정이 없는 사람의 메일에 들어갑니다.")
    @PutMapping("/team")
    fun saveTeam(
        @AdminUsername username: String,
        @RequestBody @Valid body: MailSignatureRequestBody,
    ): MailSettingsDto {
        return mailSignatureFacade.saveTeam(username, body)
    }

    @Operation(summary = "인사말·서명 미리보기", description = "저장하기 전 값으로 메일에 들어갈 모양을 그립니다.")
    @PostMapping("/preview")
    fun preview(@RequestBody @Valid body: MailSignatureRequestBody): MailPreviewDto {
        return mailSignatureFacade.preview(body)
    }
}
