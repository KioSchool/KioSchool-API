package com.kioschool.kioschoolapi.domain.donation.dto.common

import com.kioschool.kioschoolapi.domain.donation.entity.CustomerDonationClick
import java.time.LocalDateTime

data class CustomerDonationClickItemDto(
    val id: Long,
    val createdAt: LocalDateTime?,
    val orderId: Long?,
    val workspaceId: Long?,
    val workspaceName: String?,
    val method: String?,
    val amount: Int?,
    val depositAmount: Int?,
    val depositConfirmedAt: LocalDateTime?,
    val depositMemo: String?
) {
    companion object {
        fun of(click: CustomerDonationClick, workspaceName: String?) = CustomerDonationClickItemDto(
            id = click.id,
            createdAt = click.createdAt,
            orderId = click.orderId,
            workspaceId = click.workspaceId,
            workspaceName = workspaceName,
            method = click.method,
            amount = click.amount,
            depositAmount = click.depositAmount,
            depositConfirmedAt = click.depositConfirmedAt,
            depositMemo = click.depositMemo
        )
    }
}
