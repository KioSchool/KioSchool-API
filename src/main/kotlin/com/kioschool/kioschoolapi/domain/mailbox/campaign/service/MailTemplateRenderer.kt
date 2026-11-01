package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.springframework.stereotype.Component
import java.net.URLEncoder

data class MailTemplateValues(
    val workspaceName: String,
    val ownerName: String,
    val workspaceId: Long,
)

@Component
class MailTemplateRenderer {
    fun validate(subject: String, body: String, buttonLabel: String?, buttonUrl: String?) {
        val hasLabel = !buttonLabel.isNullOrBlank()
        val hasUrl = !buttonUrl.isNullOrBlank()
        if (hasLabel != hasUrl) throw invalid()

        listOfNotNull(subject, body, buttonLabel, buttonUrl).forEach { template ->
            if (PLACEHOLDER.findAll(template).any { it.value !in PLACEHOLDERS }) throw invalid()
        }
        if (hasUrl && !isSafeUrl(buttonUrl!!)) throw invalid()
    }

    fun renderText(template: String, values: MailTemplateValues): String = substitute(template, values) { it }

    fun renderUrl(template: String, values: MailTemplateValues): String = substitute(template, values) { encode(it) }

    // 정규식 한 번으로 바꾸므로 치환한 값 안의 {…}를 다시 치환하지 않는다
    private fun substitute(template: String, values: MailTemplateValues, transform: (String) -> String): String {
        val replacements = mapOf(
            WORKSPACE_NAME to values.workspaceName,
            OWNER_NAME to values.ownerName,
            WORKSPACE_ID to values.workspaceId.toString(),
        )
        return PLACEHOLDER.replace(template) { match ->
            replacements[match.value]?.let(transform) ?: match.value
        }
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
        private val PLACEHOLDERS = setOf(WORKSPACE_NAME, OWNER_NAME, WORKSPACE_ID)
        private val PLACEHOLDER = Regex("\\{[^{}]+\\}")
    }
}
