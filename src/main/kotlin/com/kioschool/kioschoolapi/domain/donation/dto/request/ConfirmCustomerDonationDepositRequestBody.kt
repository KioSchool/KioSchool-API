package com.kioschool.kioschoolapi.domain.donation.dto.request

data class ConfirmCustomerDonationDepositRequestBody(
    val amount: Int,
    val memo: String? = null,
)
