package com.kioschool.kioschoolapi.domain.donation.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "customer_donation_click")
class CustomerDonationClick(
    // 후속 분석용. 지금은 count(*)만 쓰고 아무 컬럼도 읽지 않는다.
    var workspaceId: Long? = null,
    var variant: String? = null,
    var amount: Int? = null,
) : BaseEntity()
