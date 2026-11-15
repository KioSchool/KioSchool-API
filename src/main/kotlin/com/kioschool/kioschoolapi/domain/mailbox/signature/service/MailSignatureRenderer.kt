package com.kioschool.kioschoolapi.domain.mailbox.signature.service

import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureValues
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

/**
 * 서명 값을 메일에 넣을 HTML과 글로 그린다. 모양은 Figma '서명 세부 조정'의 A-7(주황 1px 선)을 따른다.
 * 이름·영문 이름·소속/직함을 한 줄에, 연락처를 그 아래에, 로고와 한 줄 문구를 마지막 한 줄에 둔다.
 */
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
        val hasName = values.name != null || values.englishName != null
        val hasIdentity = hasName || roleLine != null
        val hasContacts = values.contacts.isNotEmpty()
        val site = baseUrl.trimEnd('/')
        val logoUrl = if (values.showLogo) "$site$LOGO_PATH" else null
        val hasBottom = logoUrl != null || values.slogan != null
        if (!hasIdentity && !hasContacts && !hasBottom) return null

        return SignatureView(
            name = values.name,
            englishName = values.englishName,
            englishNameStyle = if (values.name != null) "margin-left:4px;$ENGLISH_NAME_STYLE" else ENGLISH_NAME_STYLE,
            showSeparator = hasName && roleLine != null,
            roleLine = roleLine,
            hasIdentity = hasIdentity,
            identityStyle = paddingTop(FIRST_GAP),
            contacts = values.contacts.map { contact ->
                val label = contact.label.ifEmpty { null }
                ContactView(
                    label = label,
                    value = contact.value,
                    href = hrefOf(contact.value),
                    valueStyle = if (label != null) "margin-left:4px;$VALUE_STYLE" else VALUE_STYLE,
                )
            },
            hasContacts = hasContacts,
            contactsStyle = paddingTop(if (hasIdentity) CONTACTS_GAP else FIRST_GAP),
            hasBottom = hasBottom,
            bottomStyle = "margin-top:${if (hasIdentity || hasContacts) BOTTOM_GAP else FIRST_GAP}px;border-collapse:collapse;",
            logoUrl = logoUrl,
            siteUrl = site,
            slogan = values.slogan,
            sloganStyle = if (logoUrl != null) "padding:0 0 0 10px;$SLOGAN_STYLE" else "padding:0;$SLOGAN_STYLE",
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

    private fun paddingTop(px: Int) = "padding-top:${px}px;"

    data class SignatureView(
        val name: String?,
        val englishName: String?,
        val englishNameStyle: String,
        val showSeparator: Boolean,
        val roleLine: String?,
        val hasIdentity: Boolean,
        val identityStyle: String,
        val contacts: List<ContactView>,
        val hasContacts: Boolean,
        val contactsStyle: String,
        val hasBottom: Boolean,
        val bottomStyle: String,
        val logoUrl: String?,
        val siteUrl: String,
        val slogan: String?,
        val sloganStyle: String,
    ) {
        val containerStyle: String = CONTAINER_STYLE
        val labelStyle: String = LABEL_STYLE
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
        private const val FIRST_GAP = 12
        private const val CONTACTS_GAP = 8
        private const val BOTTOM_GAP = 10
        private const val KOREAN_FONTS = "'Apple SD Gothic Neo', 'NanumGothic', 'Malgun Gothic', sans-serif"
        private const val LATIN_FONTS = "Tahoma, $KOREAN_FONTS"
        private const val CONTAINER_STYLE = "font-family:$KOREAN_FONTS;font-size:11px;line-height:17px;color:#898989;"
        private const val ENGLISH_NAME_STYLE = "font-size:11px;color:#363636;"
        private const val LABEL_STYLE = "font-family:$LATIN_FONTS;font-weight:700;color:#888888;"
        private const val VALUE_STYLE = "font-family:$LATIN_FONTS;color:#999999;text-decoration:none;"
        private const val SLOGAN_STYLE = "vertical-align:middle;font-family:$KOREAN_FONTS;font-size:11px;line-height:17px;color:#eb6d09;"
        private val EMAIL = Regex("^[^\\s@<>\"']+@[^\\s@<>\"']+\\.[^\\s@<>\"']+$")
        private val HTTPS_URL = Regex("^https://[^\\s<>\"']+$")
        private val DOMAIN = Regex("^[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}(/[^\\s<>\"']*)?$")
    }
}
