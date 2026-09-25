package com.kioschool.kioschoolapi.domain.email.service

import com.kioschool.kioschoolapi.domain.email.entity.EmailDomain

/** 학교는 가입 이메일 도메인으로 판별한다. 등록되지 않은 도메인은 도메인 자체를 학교 이름으로 쓴다. */
class SchoolResolver(private val schoolNameByDomain: Map<String, String>) {
    // 수동 가입 등으로 이메일이 없는 유저는 한 학교로 묶는다.
    fun schoolOf(email: String?): String {
        if (email == null) return NO_EMAIL_SCHOOL_NAME
        val domain = email.substringAfter("@")
        return schoolNameByDomain[domain] ?: domain
    }

    fun domainsOf(schoolName: String): Set<String> =
        schoolNameByDomain.filterValues { it == schoolName }.keys.ifEmpty { setOf(schoolName) }

    fun domainsMatching(keyword: String?): Set<String> {
        if (keyword.isNullOrBlank()) return emptySet()
        val trimmed = keyword.trim()
        return schoolNameByDomain.filterValues { it.contains(trimmed, ignoreCase = true) }.keys
    }

    companion object {
        const val NO_EMAIL_SCHOOL_NAME = "(이메일 없음)"

        fun of(emailDomains: List<EmailDomain>): SchoolResolver =
            SchoolResolver(emailDomains.associate { it.domain to it.name })
    }
}
