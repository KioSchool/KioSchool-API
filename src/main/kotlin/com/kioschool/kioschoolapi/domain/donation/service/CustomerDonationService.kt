package com.kioschool.kioschoolapi.domain.donation.service

import com.kioschool.kioschoolapi.domain.donation.entity.CustomerDonationClick
import com.kioschool.kioschoolapi.domain.donation.repository.CustomerDonationClickRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CustomerDonationService(
    private val customerDonationClickRepository: CustomerDonationClickRepository,
) {
    @Transactional(rollbackFor = [Exception::class])
    fun recordClick(orderId: Long?, workspaceId: Long?, variant: String?, method: String?, noteIndex: Int?, amount: Int?): Long {
        customerDonationClickRepository.save(
            CustomerDonationClick(
                orderId = orderId,
                workspaceId = workspaceId,
                variant = variant,
                method = method,
                noteIndex = noteIndex,
                amount = amount,
            )
        )
        return getTodayCount()
    }

    // "오늘" = 키오스쿨 영업일(09:00 ~ 익일 08:59). DashboardFacade / OrderStatisticsService와 동일 규칙.
    fun getTodayCount(): Long =
        customerDonationClickRepository.countByCreatedAtGreaterThanEqual(startOfBusinessDay())

    fun getTotalCount(): Long = customerDonationClickRepository.count()

    private fun startOfBusinessDay(): LocalDateTime {
        val now = LocalDateTime.now()
        return if (now.hour < BUSINESS_DAY_START_HOUR) {
            now.minusDays(1).withHour(BUSINESS_DAY_START_HOUR).withMinute(0).withSecond(0).withNano(0)
        } else {
            now.withHour(BUSINESS_DAY_START_HOUR).withMinute(0).withSecond(0).withNano(0)
        }
    }

    companion object {
        private const val BUSINESS_DAY_START_HOUR = 9
    }
}
