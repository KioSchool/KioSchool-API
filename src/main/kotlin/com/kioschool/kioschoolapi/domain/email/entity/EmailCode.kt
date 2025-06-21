package com.kioschool.kioschoolapi.domain.email.entity

import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "email_code", schema = "PUBLIC")
class EmailCode(
    @Column(unique = true)
    val email: String,
    var code: String,
    var isVerified: Boolean = false,
    var kind: EmailKind
) : BaseEntity()