package com.kioschool.kioschoolapi.bank.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "bank_code", schema = "PUBLIC")
class BankCode(
    val code: String,
    val name: String
) : BaseEntity()