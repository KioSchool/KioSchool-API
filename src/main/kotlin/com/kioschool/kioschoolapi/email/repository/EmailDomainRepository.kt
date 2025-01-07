package com.kioschool.kioschoolapi.email.repository

import com.kioschool.kioschoolapi.email.entity.EmailDomain
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface EmailDomainRepository : JpaRepository<EmailDomain, Long> {
    fun findByDomain(domain: String): EmailDomain?
    fun findByNameContains(name: String, pageable: Pageable): Page<EmailDomain>
}