package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.mailbox.service.MailBodyRenderer
import com.kioschool.kioschoolapi.domain.mailbox.service.MailButton
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class MailCampaignContent(
    val subject: String,
    val bodyTemplate: String,
    val buttonLabel: String?,
    val buttonUrlTemplate: String?,
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
    fun compose(content: MailCampaignContent, values: MailTemplateValues, unsubscribeToken: String): ComposedMail {
        val subject = mailTemplateRenderer.renderText(content.subject, values)
        val body = mailTemplateRenderer.renderText(content.bodyTemplate, values)
        val button = buttonOf(content, values)
        val unsubscribeUrl = "$baseUrl/mail/unsubscribe?token=$unsubscribeToken"

        return ComposedMail(
            subject = subject,
            html = mailBodyRenderer.render(body, button, unsubscribeUrl),
            text = textOf(body, button, unsubscribeUrl),
            headers = mapOf(
                "List-Unsubscribe" to "<$apiBaseUrl/mail/unsubscribe?token=$unsubscribeToken>",
                "List-Unsubscribe-Post" to "List-Unsubscribe=One-Click",
            ),
        )
    }

    private fun buttonOf(content: MailCampaignContent, values: MailTemplateValues): MailButton? {
        val label = content.buttonLabel ?: return null
        val url = content.buttonUrlTemplate ?: return null
        return MailButton(mailTemplateRenderer.renderText(label, values), mailTemplateRenderer.renderUrl(url, values))
    }

    private fun textOf(body: String, button: MailButton?, unsubscribeUrl: String): String {
        val buttonLine = button?.let { "\n\n${it.label}: ${it.url}" }.orEmpty()
        return "$body$buttonLine\n\n수신 거부: $unsubscribeUrl"
    }
}
