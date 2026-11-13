package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.mailbox.service.MailBodyRenderer
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class MailCampaignContent(
    val subject: String,
    val bodyTemplate: String,
    /** 본문의 서명 자리에 넣을 팀 공용 서명. 발송을 만들 때 값을 저장해 두고 끝까지 같은 서명을 쓴다. */
    val signature: MailSignatureBlock? = null,
)

data class ComposedMail(
    val subject: String,
    val html: String,
    val text: String,
    val headers: Map<String, String>,
)

@Component
class MailCampaignComposer(
    private val mailTemplateRenderer: MailTemplateRenderer,
    private val mailBodyRenderer: MailBodyRenderer,
    @Value("\${kioschool.base-url}")
    private val baseUrl: String,
    @Value("\${mail.api-base-url}")
    private val apiBaseUrl: String,
) {
    /** 받을 주점이 아직 없는 미리보기는 [values] 없이 값 자리를 그대로 둔다. */
    fun compose(content: MailCampaignContent, values: MailTemplateValues?, unsubscribeToken: String): ComposedMail {
        val subject = values?.let { mailTemplateRenderer.renderText(content.subject, it) } ?: content.subject
        val body = values?.let { mailTemplateRenderer.renderHtml(content.bodyTemplate, it) } ?: content.bodyTemplate
        val unsubscribeUrl = "$baseUrl/mail/unsubscribe?token=$unsubscribeToken"

        return ComposedMail(
            subject = subject,
            html = mailBodyRenderer.render(body, unsubscribeUrl, content.signature),
            text = "${mailBodyRenderer.renderText(body, content.signature)}\n\n수신 거부: $unsubscribeUrl",
            headers = mapOf(
                "List-Unsubscribe" to "<$apiBaseUrl/mail/unsubscribe?token=$unsubscribeToken>",
                "List-Unsubscribe-Post" to "List-Unsubscribe=One-Click",
            ),
        )
    }
}
