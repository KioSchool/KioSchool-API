package com.kioschool.kioschoolapi.domain.account.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "account", schema = "PUBLIC")
class Account(
    @ManyToOne
    var bank: Bank,
    var accountNumber: String,
    var accountHolder: String,
    var tossAccountUrl: String? = null
) : BaseEntity()