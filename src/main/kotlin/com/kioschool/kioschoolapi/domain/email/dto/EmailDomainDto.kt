package com.kioschool.kioschoolapi.domain.email.dto

import com.kioschool.kioschoolapi.domain.email.entity.EmailDomain
import java.time.LocalDateTime

data class EmailDomainDto(
    val id: Long,
    val name: String,
    val domain: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(emailDomain: EmailDomain): EmailDomainDto {
            return EmailDomainDto(
                id = emailDomain.id,
                name = emailDomain.name,
                domain = emailDomain.domain,
                createdAt = emailDomain.createdAt,
                updatedAt = emailDomain.updatedAt
            )
        }
    }
}
