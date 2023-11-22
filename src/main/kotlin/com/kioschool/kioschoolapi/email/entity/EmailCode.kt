package com.kioschool.kioschoolapi.email.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "email_code", schema = "PUBLIC")
class EmailCode(
    @Column(unique = true)
    val email: String,
    var code: String,
) : BaseEntity()