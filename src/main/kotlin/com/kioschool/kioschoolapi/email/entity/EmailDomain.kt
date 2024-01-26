package com.kioschool.kioschoolapi.email.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "email_domain")
class EmailDomain(val domain: String) : BaseEntity()