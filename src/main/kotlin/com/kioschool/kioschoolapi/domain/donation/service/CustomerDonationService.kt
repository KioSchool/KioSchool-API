package com.kioschool.kioschoolapi.domain.donation.service

import com.kioschool.kioschoolapi.domain.donation.entity.CustomerDonationClick
import com.kioschool.kioschoolapi.domain.donation.repository.CustomerDonationClickRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class CustomerDonationService(
    private val customerDonationClickRepository: CustomerDonationClickRepository,
) {
    @Transactional(rollbackFor = [Exception::class])
    fun recordClick(workspaceId: Long?, variant: String?, amount: Int?): Long {
        customerDonationClickRepository.save(
            CustomerDonationClick(
                workspaceId = workspaceId,
                variant = variant,
                amount = amount,
            )
        )
        return getTodayCount()
    }

    fun getTodayCount(): Long =
        customerDonationClickRepository.countByCreatedAtGreaterThanEqual(LocalDate.now().atStartOfDay())
}
