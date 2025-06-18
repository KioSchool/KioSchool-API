package com.kioschool.kioschoolapi.domain.account.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "bank", schema = "PUBLIC")
class Bank(
    val name: String,
    val code: String
) : BaseEntity()