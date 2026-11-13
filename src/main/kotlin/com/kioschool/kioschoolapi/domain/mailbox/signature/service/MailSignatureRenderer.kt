package com.kioschool.kioschoolapi.domain.mailbox.signature.service

import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureValues
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

/** 서명 값을 메일에 넣을 HTML과 글로 그린다. 모양은 Figma '메일 인사말·서명'의 A-4를 따른다. */
@Component
class MailSignatureRenderer(
    private val templateEngine: SpringTemplateEngine,
    @Value("\${kioschool.base-url}")
    private val baseUrl: String,
) {
    /** 그릴 값이 하나도 없으면 null. 메일의 서명 자리는 지워진다. */
    fun render(values: MailSignatureValues): MailSignatureBlock? {
        val view = toView(values.normalized()) ?: return null
        val html = templateEngine.process(TEMPLATE, Context().apply { setVariable("s", view) }).trim()
        return MailSignatureBlock(html, renderText(view))
    }

    private fun toView(values: MailSignatureValues): SignatureView? {
        val roleLine = listOfNotNull(values.organization, values.jobTitle).joinToString(" / ").ifEmpty { null }
        val hasIdentity = values.name != null || values.englishName != null || roleLine != null
        val hasDetails = values.contacts.isNotEmpty() || values.slogan != null
        val logoUrl = if (values.showLogo) "${baseUrl.trimEnd('/')}$LOGO_PATH" else null
        if (!hasIdentity && !hasDetails && logoUrl == null) return null

        return SignatureView(
            name = values.name,
            englishName = values.englishName,
            englishNameStyle = if (values.name != null) "margin-left:8px;$ENGLISH_NAME_STYLE" else ENGLISH_NAME_STYLE,
            roleLine = roleLine,
            hasIdentity = hasIdentity,
            contacts = values.contacts.map { contact ->
                val label = contact.label.ifEmpty { null }
                ContactView(
                    label = label,
                    value = contact.value,
                    href = hrefOf(contact.value),
                    valueStyle = if (label != null) "margin-left:5px;$VALUE_STYLE" else VALUE_STYLE,
                )
            },
            slogan = values.slogan,
            hasDetails = hasDetails,
            detailsStyle = if (hasIdentity) "padding-top:20px;" else "padding-top:24px;",
            logoUrl = logoUrl,
            logoStyle = if (hasIdentity || hasDetails) "padding-top:22px;" else "padding-top:24px;",
        )
    }

    private fun renderText(view: SignatureView): String {
        val lines = buildList {
            add(TEXT_DELIMITER)
            listOfNotNull(view.name, view.englishName).joinToString(" ").ifEmpty { null }?.let(::add)
            view.roleLine?.let(::add)
            view.contacts.forEach { add(listOfNotNull(it.label, it.value).joinToString(" ")) }
            view.slogan?.let(::add)
        }
        return lines.joinToString("\n")
    }

    // 메일 프로그램이 주소를 파란 링크로 바꾸면 회색 서명이 깨지므로, 링크가 될 값은 직접 색을 정한 링크로 감싼다
    private fun hrefOf(value: String): String? = when {
        EMAIL.matches(value) -> "mailto:$value"
        HTTPS_URL.matches(value) -> value
        DOMAIN.matches(value) -> "https://$value"
        else -> null
    }

    data class SignatureView(
        val name: String?,
        val englishName: String?,
        val englishNameStyle: String,
        val roleLine: String?,
        val hasIdentity: Boolean,
        val contacts: List<ContactView>,
        val slogan: String?,
        val hasDetails: Boolean,
        val detailsStyle: String,
        val logoUrl: String?,
        val logoStyle: String,
    ) {
        val containerStyle: String = CONTAINER_STYLE
    }

    data class ContactView(
        val label: String?,
        val value: String,
        val href: String?,
        val valueStyle: String,
    )

    companion object {
        private const val TEMPLATE = "mailSignature"
        const val LOGO_PATH = "/mail/kio-logo.png"
        private const val TEXT_DELIMITER = "--"
        private const val CONTAINER_STYLE =
            "font-family:'Apple SD Gothic Neo', 'NanumGothic', 'Malgun Gothic', sans-serif;font-size:14px;line-height:1.65;color:#888888;"
        private const val ENGLISH_NAME_STYLE = "font-size:14px;color:#333333;"
        private const val VALUE_STYLE = "color:#888888;text-decoration:none;"
        private val EMAIL = Regex("^[^\\s@<>\"']+@[^\\s@<>\"']+\\.[^\\s@<>\"']+$")
        private val HTTPS_URL = Regex("^https://[^\\s<>\"']+$")
        private val DOMAIN = Regex("^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}(/[^\\s<>\"']*)?$")
    }
}
