package com.kioschool.kioschoolapi.domain.donation.dto.request

data class RecordCustomerDonationClickRequestBody(
    val orderId: Long? = null,
    val workspaceId: Long? = null,
    val variant: String? = null,
    val method: String? = null,
    val noteIndex: Int? = null,
    val amount: Int? = null,
)
