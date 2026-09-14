package com.kioschool.kioschoolapi.domain.donation.controller

import com.kioschool.kioschoolapi.domain.donation.dto.common.CustomerDonationClickStatsDto
import com.kioschool.kioschoolapi.domain.donation.facade.SuperAdminDonationFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Super Admin Donation Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminDonationController(
    private val superAdminDonationFacade: SuperAdminDonationFacade
) {
    @Operation(
        summary = "손님 후원 클릭 현황",
        description = "기간(영업일 기준, 양 끝 포함, 최대 366일) 내 후원 버튼 클릭을 일자·금액·수단·문구·주점별로 집계합니다.<br>클릭은 송금 시도이며 실제 입금 여부는 알 수 없습니다."
    )
    @GetMapping("/donations/customer-clicks")
    fun getCustomerDonationClickStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): CustomerDonationClickStatsDto {
        return superAdminDonationFacade.getCustomerClickStats(startDate, endDate)
    }
}
