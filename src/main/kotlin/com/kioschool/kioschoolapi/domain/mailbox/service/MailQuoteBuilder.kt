package com.kioschool.kioschoolapi.domain.mailbox.service

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MailQuote(val html: String, val text: String)

// 답장에 가장 최근 메일을 붙인다. 그 메일에 붙은 인용까지 따라오므로 대화 전체가 이어진다
@Component
class MailQuoteBuilder(
    private val mailHtmlSanitizer: MailHtmlSanitizer,
) {
    fun build(message: MailMessage): MailQuote? {
        val body = mailHtmlSanitizer.parseQuote(sourceHtml(message))
        if (body.text().isBlank() && body.select("img").isEmpty()) return null

        body.select("p").attr("style", PARAGRAPH_STYLE)
        body.select("blockquote").attr("style", BLOCKQUOTE_STYLE)
        val header = headerOf(message)
        val quotedLines = textOf(message).lines().joinToString("\n") { if (it.isEmpty()) ">" else "> $it" }
        return MailQuote(
            html = """<p style="$HEADER_STYLE">${Entities.escape(header)}</p>""" +
                """<blockquote style="$BLOCKQUOTE_STYLE">${body.body().html()}</blockquote>""",
            text = "$header\n$quotedLines",
        )
    }

    // 대화 화면은 메일을 차례로 보여 주므로, 보낸 메일에 우리가 붙인 인용까지 보이면 같은 내용이 반복된다
    fun visibleHtml(message: MailMessage): String? {
        val html = message.htmlBody
        if (html == null || message.direction != MailDirection.OUTBOUND || QUOTE_ATTRIBUTE !in html) return html

        val document = Jsoup.parse(html)
        document.outputSettings().prettyPrint(false)
        document.select("[$QUOTE_ATTRIBUTE]").remove()
        return document.outerHtml()
    }

    private fun sourceHtml(message: MailMessage): String {
        val html = message.htmlBody
        if (html.isNullOrBlank()) return Entities.escape(message.textBody.orEmpty()).replace("\n", "<br>")

        val document = Jsoup.parse(html)
        document.outputSettings().prettyPrint(false)
        // 상대가 우리 메일을 인용하면 표시 속성이 남을 수 있어, 우리가 보낸 메일에서만 내용을 골라낸다
        val ours = document.select("[$CONTENT_ATTRIBUTE], [$QUOTE_ATTRIBUTE]").takeIf { message.direction == MailDirection.OUTBOUND }
        return if (ours.isNullOrEmpty()) document.body().html() else ours.outerHtml()
    }

    private fun textOf(message: MailMessage): String {
        val text = message.textBody
        if (!text.isNullOrBlank()) return text.trim()

        val document = Jsoup.parseBodyFragment(sourceHtml(message))
        document.select("p, div, li, tr, h1, h2, h3, h4, blockquote").after("\n")
        return document.body().wholeText().replace(BLANK_LINES, "\n\n").trim()
    }

    private fun headerOf(message: MailMessage): String {
        val sender = listOfNotNull(message.fromName?.takeIf { it.isNotBlank() }, message.fromEmail?.let { "<$it>" }).joinToString(" ")
        val author = if (sender.isEmpty()) "이전 메일:" else "${sender}님이 작성:"
        return listOfNotNull(message.sentAt?.format(HEADER_DATE), author).joinToString(", ")
    }

    companion object {
        const val CONTENT_ATTRIBUTE = "data-mail-content"
        const val QUOTE_ATTRIBUTE = "data-mail-quote"
        private const val HEADER_STYLE = "margin:0 0 8px;font-size:13px;color:#6b7280;"
        private const val PARAGRAPH_STYLE = "margin:0 0 8px;"
        private const val BLOCKQUOTE_STYLE = "margin:0;padding:0 0 0 12px;border-left:2px solid #e5e7eb;"
        private val HEADER_DATE = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E) a h:mm", Locale.KOREAN)
        private val BLANK_LINES = Regex("\n{3,}")
    }
}
