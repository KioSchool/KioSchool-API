package com.kioschool.kioschoolapi.domain.donation.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "customer_donation_click")
class CustomerDonationClick(
    var orderId: Long? = null,
    var workspaceId: Long? = null,
    var variant: String? = null,
    var method: String? = null,
    var noteIndex: Int? = null,
    var amount: Int? = null,
    // 슈퍼어드민이 통장 내역과 대조해 실제 입금을 확인한 금액. null이면 미확인.
    // 클릭 금액(amount)과 다를 수 있고, 금액을 모르는 계좌이체 클릭에도 채울 수 있다.
    var depositAmount: Int? = null,
    var depositConfirmedAt: LocalDateTime? = null,
    var depositMemo: String? = null,
) : BaseEntity() {
    val isDeposited: Boolean
        get() = depositAmount != null

    fun confirmDeposit(amount: Int, memo: String?, confirmedAt: LocalDateTime) {
        depositAmount = amount
        depositMemo = memo
        depositConfirmedAt = confirmedAt
    }

    fun cancelDeposit() {
        depositAmount = null
        depositMemo = null
        depositConfirmedAt = null
    }
}
