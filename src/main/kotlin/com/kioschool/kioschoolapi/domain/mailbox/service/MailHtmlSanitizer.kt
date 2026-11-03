package com.kioschool.kioschoolapi.domain.mailbox.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.jsoup.safety.Safelist
import org.springframework.stereotype.Component

// 편집기 내용은 브라우저가 보낸 값이라 그대로 믿지 않는다. 메일에 쓰는 태그와 https 링크만 남긴다.
@Component
class MailHtmlSanitizer {
    private val safelist = Safelist()
        .addTags("p", "br", "strong", "b", "em", "i", "u", "h2", "h3", "ul", "ol", "li", "hr", "a")
        .addAttributes("a", "href", BUTTON_ATTRIBUTE)
        .addAttributes("p", ALIGN_ATTRIBUTE)
        .addAttributes("h2", ALIGN_ATTRIBUTE)
        .addAttributes("h3", ALIGN_ATTRIBUTE)
        .addProtocols("a", "href", "https")

    fun clean(html: String): String {
        val document = parseBody(Jsoup.clean(html, "", safelist, mailOutputSettings()))
        document.select("a[$BUTTON_ATTRIBUTE]").forEach {
            if (it.attr(BUTTON_ATTRIBUTE) !in BUTTON_VARIANTS) it.removeAttr(BUTTON_ATTRIBUTE)
        }
        document.select("[$ALIGN_ATTRIBUTE]").forEach {
            if (it.attr(ALIGN_ATTRIBUTE) !in ALIGNMENTS) it.removeAttr(ALIGN_ATTRIBUTE)
        }
        return document.body().html()
    }

    fun parse(html: String): Document = parseBody(clean(html))

    // 인용하는 메일은 밖에서 온 HTML일 수 있어, 글과 표 모양만 남기고 스크립트와 스타일은 지운다
    fun parseQuote(html: String): Document {
        val document = parseBody(Jsoup.clean(html, "", Safelist.relaxed(), mailOutputSettings()))
        document.select("img:not([src])").remove()
        return document
    }

    fun isBlank(html: String): Boolean {
        val document = parse(html)
        return document.text().replace(' ', ' ').isBlank() && document.select("a[$BUTTON_ATTRIBUTE][href]").isEmpty()
    }

    // https가 아닌 주소는 clean이 지우므로, 주소 없는 링크가 남으면 쓸 수 없는 링크가 있었다는 뜻이다
    fun hasInvalidLink(html: String): Boolean = parse(html).select("a:not([href])").isNotEmpty()

    private fun parseBody(html: String): Document = Jsoup.parseBodyFragment(html).apply { outputSettings(mailOutputSettings()) }

    private fun mailOutputSettings() = Document.OutputSettings()
        .prettyPrint(false)
        .escapeMode(Entities.EscapeMode.base)
        .charset(Charsets.UTF_8)

    companion object {
        const val BUTTON_ATTRIBUTE = "data-mail-button"
        const val BUTTON_SOLID = "solid"
        const val BUTTON_LINE = "line"
        private val BUTTON_VARIANTS = setOf(BUTTON_SOLID, BUTTON_LINE)
        const val ALIGN_ATTRIBUTE = "data-align"
        private val ALIGNMENTS = setOf("center", "right")
    }
}
