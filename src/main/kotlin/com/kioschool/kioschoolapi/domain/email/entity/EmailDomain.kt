package com.kioschool.kioschoolapi.domain.email.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "email_domain")
class EmailDomain(
    val name: String,
    val domain: String
) : BaseEntity()