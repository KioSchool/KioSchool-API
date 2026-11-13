package com.kioschool.kioschoolapi.domain.mailbox.signature.facade

import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailPreviewDto
import com.kioschool.kioschoolapi.domain.mailbox.service.MailBodyRenderer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer.Companion.SIGNATURE_ATTRIBUTE
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSettingsDto
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.signature.service.MailSignatureRenderer
import com.kioschool.kioschoolapi.domain.mailbox.signature.service.MailSignatureService
import com.kioschool.kioschoolapi.domain.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MailSignatureFacade(
    private val mailSignatureService: MailSignatureService,
    private val mailSignatureRenderer: MailSignatureRenderer,
    private val mailBodyRenderer: MailBodyRenderer,
    private val userService: UserService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getSettings(username: String): MailSettingsDto {
        val user = userService.getUser(username)
        val mine = mailSignatureService.findMine(user)
        val team = mailSignatureService.getTeam()
        return MailSettingsDto(
            mine = mine?.let { mailSignatureService.toDto(it) },
            team = mailSignatureService.toDto(team),
            personalSignatureHtml = mailSignatureService.render(mine ?: team)?.html,
            teamSignatureHtml = mailSignatureService.render(team)?.html,
            myAccountName = user.name,
        )
    }

    fun saveMine(username: String, body: MailSignatureRequestBody): MailSettingsDto {
        mailSignatureService.saveMine(userService.getUser(username), body)
        return getSettings(username)
    }

    fun saveTeam(username: String, body: MailSignatureRequestBody): MailSettingsDto {
        mailSignatureService.saveTeam(body)
        log.info("[AUDIT] action=UPDATE_TEAM_MAIL_SIGNATURE by={}", username)
        return getSettings(username)
    }

    /** 저장 전 값으로, 새 메일에 들어갈 모양(인사말 + 빈 본문 + 서명)을 그린다. */
    fun preview(body: MailSignatureRequestBody): MailPreviewDto {
        val greeting = mailSignatureService.cleanGreeting(body.greetingHtml)
        val content = "$greeting<p></p><div $SIGNATURE_ATTRIBUTE=\"\"></div>"
        val signature = mailSignatureRenderer.render(body.toValues())
        return MailPreviewDto(mailBodyRenderer.renderPersonal(content, signature = signature))
    }
}
