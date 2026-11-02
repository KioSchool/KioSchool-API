package com.kioschool.kioschoolapi.domain.mailbox.service

import jakarta.mail.internet.MimeUtility
import java.io.UnsupportedEncodingException

data class ParsedMailAddress(
    val name: String?,
    val email: String,
)

object MailAddressParser {
    private val NAMED_ADDRESS = Regex("^(.*)<([^<>]+)>\\s*$", RegexOption.DOT_MATCHES_ALL)

    fun parse(raw: String): ParsedMailAddress {
        val match = NAMED_ADDRESS.find(raw.trim()) ?: return ParsedMailAddress(null, raw.trim().lowercase())
        val (name, email) = match.destructured
        return ParsedMailAddress(decodeName(name), email.trim().lowercase())
    }

    private fun decodeName(raw: String): String? {
        val name = raw.trim().removeSurrounding("\"").trim()
        if (name.isEmpty()) return null
        return try {
            MimeUtility.decodeText(name)
        } catch (e: UnsupportedEncodingException) {
            name
        }
    }
}
