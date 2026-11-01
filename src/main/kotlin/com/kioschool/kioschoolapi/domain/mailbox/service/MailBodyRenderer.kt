package com.kioschool.kioschoolapi.domain.mailbox.service

import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

data class MailButton(
    val label: String,
    val url: String,
)

@Service
class MailBodyRenderer(
    private val templateEngine: SpringTemplateEngine,
) {
    fun render(content: String, button: MailButton? = null, unsubscribeUrl: String? = null): String {
        val context = Context().apply {
            setVariable("contentHtml", toHtml(content))
            setVariable("button", button)
            setVariable("unsubscribeUrl", unsubscribeUrl)
        }
        return templateEngine.process(TEMPLATE, context)
    }

    private fun toHtml(content: String): String =
        HtmlUtils.htmlEscape(content.replace("\r\n", "\n"), Charsets.UTF_8.name()).replace("\n", "<br>")

    companion object {
        private const val TEMPLATE = "mailMessage"
    }
}
