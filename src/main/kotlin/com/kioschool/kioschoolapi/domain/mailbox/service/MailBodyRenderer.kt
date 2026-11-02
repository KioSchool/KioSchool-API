package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer.Companion.BUTTON_ATTRIBUTE
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer.Companion.BUTTON_LINE
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer.Companion.BUTTON_SOLID
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class MailBodyRenderer(
    private val templateEngine: SpringTemplateEngine,
    private val mailHtmlSanitizer: MailHtmlSanitizer,
) {
    fun render(contentHtml: String, unsubscribeUrl: String? = null): String {
        val context = Context().apply {
            setVariable("contentHtml", styled(contentHtml))
            setVariable("unsubscribeUrl", unsubscribeUrl)
        }
        return templateEngine.process(TEMPLATE, context)
    }

    fun renderText(contentHtml: String): String =
        mailHtmlSanitizer.parse(contentHtml).body().children()
            .mapNotNull { blockText(it) }
            .joinToString("\n\n")

    // 메일 프로그램은 <style>을 지우는 곳이 많아 태그마다 스타일을 직접 넣는다
    private fun styled(contentHtml: String): String {
        val document = mailHtmlSanitizer.parse(contentHtml)
        STYLES.forEach { (selector, style) -> document.select(selector).attr("style", style) }
        document.select("a[$BUTTON_ATTRIBUTE]").removeAttr(BUTTON_ATTRIBUTE)
        return document.body().html()
    }

    private fun blockText(element: Element): String? = when (element.tagName()) {
        "hr" -> HORIZONTAL_RULE
        "ul" -> element.children().joinToString("\n") { "- ${lineText(it)}" }
        "ol" -> element.children().withIndex().joinToString("\n") { (index, item) -> "${index + 1}. ${lineText(item)}" }
        else -> lineText(element).ifBlank { null }
    }

    private fun lineText(element: Element): String = inlineText(element).trimEnd(' ')

    private fun inlineText(node: Node): String = when {
        node is TextNode -> node.wholeText
        node is Element && node.tagName() == "br" -> "\n"
        node is Element && node.tagName() == "a" -> linkText(node)
        else -> node.childNodes().joinToString("") { inlineText(it) }
    }

    private fun linkText(link: Element): String {
        val label = link.text()
        val href = link.attr("href")
        return when {
            link.hasAttr(BUTTON_ATTRIBUTE) -> "$label: $href"
            href.isEmpty() || href == label -> label
            else -> "$label ($href)"
        }
    }

    companion object {
        private const val TEMPLATE = "mailMessage"
        private const val HORIZONTAL_RULE = "---"
        private const val BUTTON_BASE =
            "display:inline-block;padding:12px 20px;border-radius:8px;font-size:14px;font-weight:600;text-decoration:none;"

        // 뒤의 선택자가 앞의 스타일을 덮어쓴다
        private val STYLES = listOf(
            "p" to "margin:0 0 12px;",
            "h2" to "margin:0 0 12px;font-size:18px;font-weight:700;color:#111827;",
            "h3" to "margin:0 0 10px;font-size:16px;font-weight:700;color:#111827;",
            "ul, ol" to "margin:0 0 12px;padding-left:20px;",
            "li" to "margin:0 0 4px;",
            "li > p" to "margin:0;",
            "a" to "color:#eb6d09;",
            "hr" to "margin:20px 0;border:0;border-top:1px solid #e5e7eb;",
            "a[$BUTTON_ATTRIBUTE=$BUTTON_SOLID]" to "${BUTTON_BASE}background:#eb6d09;color:#ffffff;",
            "a[$BUTTON_ATTRIBUTE=$BUTTON_LINE]" to "${BUTTON_BASE}background:#ffffff;color:#eb6d09;border:1px solid #eb6d09;",
        )
    }
}
