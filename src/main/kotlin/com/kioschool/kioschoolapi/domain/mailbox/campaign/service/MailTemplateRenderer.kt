package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.time.LocalDate

// 시작할 때 수신자 행에 고정한다. 며칠에 나눠 보내도 목록에서 본 값과 메일 값이 같다.
data class MailAudienceStats(
    val schoolName: String = "",
    val tableCount: Int = 0,
    val orderCount: Long = 0,
    val revenue: Long = 0,
    val operatingDays: Long = 0,
    val firstOperatingDate: LocalDate? = null,
    val lastOperatingDate: LocalDate? = null,
    val topMenu: String? = null,
)

data class MailTemplateValues(
    val workspaceName: String,
    val ownerName: String,
    val workspaceId: Long,
    val stats: MailAudienceStats = MailAudienceStats(),
)

@Component
class MailTemplateRenderer(
    private val mailHtmlSanitizer: MailHtmlSanitizer,
) {
    fun validate(subject: String, bodyHtml: String) {
        validateValues(subject, bodyHtml)
        if (mailHtmlSanitizer.isBlank(bodyHtml)) throw invalid()
    }

    fun validateValues(subject: String, bodyHtml: String) {
        if (hasUnknownValue(subject)) throw invalid()

        val document = mailHtmlSanitizer.parse(bodyHtml)
        if (hasUnknownValue(document.text()) || hasSplitValue(document)) throw invalid()
        if (document.select("a:not([href])").isNotEmpty()) throw invalid()
        document.select("a[href]").map { it.attr("href") }.forEach { href ->
            if (hasUnknownValue(href) || !isSafeUrl(href)) throw invalid()
        }
    }

    fun renderText(template: String, values: MailTemplateValues): String = substitute(template, values) { it }

    fun renderUrl(template: String, values: MailTemplateValues): String = substitute(template, values) { encode(it) }

    // 문서 구조를 고정한 채 글자 노드와 주소만 바꾸므로, 값에 든 태그는 글자로만 나간다
    fun renderHtml(bodyHtml: String, values: MailTemplateValues): String {
        val document = mailHtmlSanitizer.parse(bodyHtml)
        document.body().select("*").forEach { element ->
            element.textNodes().forEach { it.text(renderText(it.wholeText, values)) }
        }
        document.select("a[href]").forEach { it.attr("href", renderUrl(it.attr("href"), values)) }
        return document.body().html()
    }

    private fun substitute(template: String, values: MailTemplateValues, transform: (String) -> String): String {
        val replacements = replacementsOf(values)
        return PLACEHOLDER.replace(template) { match ->
            replacements[match.value]?.let(transform) ?: match.value
        }
    }

    private fun replacementsOf(values: MailTemplateValues): Map<String, String> {
        val stats = values.stats
        return mapOf(
            WORKSPACE_NAME to values.workspaceName,
            OWNER_NAME to values.ownerName,
            WORKSPACE_ID to values.workspaceId.toString(),
            SCHOOL_NAME to stats.schoolName,
            TABLE_COUNT to stats.tableCount.toString(),
            ORDER_COUNT to "%,d".format(stats.orderCount),
            REVENUE to "%,d원".format(stats.revenue),
            OPERATING_DAYS to stats.operatingDays.toString(),
            FIRST_OPERATING_DATE to formatDate(stats.firstOperatingDate),
            LAST_OPERATING_DATE to formatDate(stats.lastOperatingDate),
            TOP_MENU to stats.topMenu.orEmpty(),
        )
    }

    private fun formatDate(date: LocalDate?): String = date?.let { "${it.monthValue}월 ${it.dayOfMonth}일" }.orEmpty()

    private fun hasUnknownValue(text: String): Boolean = PLACEHOLDER.findAll(text).any { it.value !in VALUES }

    // 값 일부에만 꾸미기를 걸면 글자 노드가 나뉘어 치환되지 않은 채 나간다
    private fun hasSplitValue(document: Document): Boolean {
        val textNodes: List<TextNode> = document.body().select("*").flatMap { it.textNodes() }
        val inNodes = textNodes.sumOf { node -> PLACEHOLDER.findAll(node.wholeText).count() }
        return PLACEHOLDER.findAll(document.text()).count() != inNodes
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    private fun isSafeUrl(url: String): Boolean {
        if (!url.startsWith(HTTPS)) return false
        val host = url.removePrefix(HTTPS).takeWhile { it !in "/?#" }
        if (host.isBlank() || '{' in host) return false
        return PLACEHOLDER.replace(url, "").none { it.isWhitespace() }
    }

    private fun invalid() = CustomException(ErrorCode.MAIL_TEMPLATE_INVALID)

    companion object {
        private const val HTTPS = "https://"
        private const val WORKSPACE_NAME = "{주점 이름}"
        private const val OWNER_NAME = "{사장 이름}"
        private const val WORKSPACE_ID = "{주점 번호}"
        private const val SCHOOL_NAME = "{학교}"
        private const val TABLE_COUNT = "{테이블 수}"
        private const val ORDER_COUNT = "{주문 수}"
        private const val REVENUE = "{매출}"
        private const val OPERATING_DAYS = "{운영일}"
        private const val FIRST_OPERATING_DATE = "{첫 운영일}"
        private const val LAST_OPERATING_DATE = "{마지막 운영일}"
        private const val TOP_MENU = "{인기 메뉴}"
        private val VALUES = setOf(
            WORKSPACE_NAME, OWNER_NAME, WORKSPACE_ID, SCHOOL_NAME, TABLE_COUNT, ORDER_COUNT,
            REVENUE, OPERATING_DAYS, FIRST_OPERATING_DATE, LAST_OPERATING_DATE, TOP_MENU,
        )
        private val PLACEHOLDER = Regex("\\{[^{}]+\\}")
    }
}
