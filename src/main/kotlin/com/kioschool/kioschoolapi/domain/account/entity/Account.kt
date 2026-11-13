package com.kioschool.kioschoolapi.domain.account.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "account", schema = "PUBLIC")
class Account(
    @ManyToOne(fetch = FetchType.LAZY)
    var bank: Bank,
    var accountNumber: String,
    var accountHolder: String,
    var tossAccountUrl: String? = null,
    // null = 카카오페이 송금 꺼짐. 운영진이 켜야 손님 결제 화면에 보인다.
    var kakaoPayAccountUrl: String? = null
) : BaseEntity()