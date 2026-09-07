package com.kioschool.kioschoolapi.domain.donation.dto.request

data class RecordCustomerDonationClickRequestBody(
    val workspaceId: Long? = null,
    val variant: String? = null,
    val amount: Int? = null,
)
