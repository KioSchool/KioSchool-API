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
            workspaceId = body.workspaceId,
            variant = body.variant?.trim()?.takeIf { it.isNotEmpty() },
            amount = body.amount,
        )
        return CustomerDonationClickCountResponse(todayCount)
    }

    fun getTodayCount(): CustomerDonationClickCountResponse =
        CustomerDonationClickCountResponse(customerDonationService.getTodayCount())
}
