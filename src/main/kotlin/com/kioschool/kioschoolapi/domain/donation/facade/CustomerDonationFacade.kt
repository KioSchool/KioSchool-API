package com.kioschool.kioschoolapi.domain.donation.facade

import com.kioschool.kioschoolapi.domain.donation.dto.common.CustomerDonationClickCountResponse
import com.kioschool.kioschoolapi.domain.donation.dto.request.RecordCustomerDonationClickRequestBody
import com.kioschool.kioschoolapi.domain.donation.service.CustomerDonationService
import org.springframework.stereotype.Component

@Component
class CustomerDonationFacade(
    private val customerDonationService: CustomerDonationService,
) {
    fun recordClick(body: RecordCustomerDonationClickRequestBody): CustomerDonationClickCountResponse {
        val todayCount = customerDonationService.recordClick(
            orderId = body.orderId,
            workspaceId = body.workspaceId,
            variant = body.variant?.trimToNull(),
            method = body.method?.trimToNull(),
            noteIndex = body.noteIndex,
            amount = body.amount,
        )
        return CustomerDonationClickCountResponse(todayCount, customerDonationService.getTotalCount())
    }

    fun getCounts(): CustomerDonationClickCountResponse =
        CustomerDonationClickCountResponse(customerDonationService.getTodayCount(), customerDonationService.getTotalCount())

    private fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }
}
