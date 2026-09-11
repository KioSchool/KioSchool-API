package com.kioschool.kioschoolapi.domain.donation.repository

import com.kioschool.kioschoolapi.domain.donation.entity.CustomerDonationClick
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CustomerDonationClickRepository : JpaRepository<CustomerDonationClick, Long> {
    fun countByCreatedAtGreaterThanEqual(start: LocalDateTime): Long
}
