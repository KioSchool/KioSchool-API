package com.kioschool.kioschoolapi.domain.donation.controller

import com.kioschool.kioschoolapi.domain.donation.dto.common.CustomerDonationClickItemDto
import com.kioschool.kioschoolapi.domain.donation.dto.common.CustomerDonationClickStatsDto
import com.kioschool.kioschoolapi.domain.donation.dto.request.ConfirmCustomerDonationDepositRequestBody
import com.kioschool.kioschoolapi.domain.donation.facade.SuperAdminDonationFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
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
        description = "기간(영업일 기준, 양 끝 포함, 최대 366일) 내 후원 버튼 클릭을 일자·금액·수단·문구·주점별로 집계합니다.<br>클릭은 송금 시도이며, 실제 입금은 슈퍼어드민이 확인 표시한 건만 deposit 항목에 집계됩니다."
    )
    @GetMapping("/donations/customer-clicks")
    fun getCustomerDonationClickStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): CustomerDonationClickStatsDto {
        return superAdminDonationFacade.getCustomerClickStats(startDate, endDate)
    }

    @Operation(
        summary = "손님 후원 클릭 목록",
        description = "영업일(09:00 ~ 익일 08:59) 하루의 후원 버튼 클릭을 시각 순으로 조회합니다. 통장 입금 내역과 대조할 때 사용합니다."
    )
    @GetMapping("/donations/customer-clicks/items")
    fun getCustomerDonationClickItems(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): List<CustomerDonationClickItemDto> {
        return superAdminDonationFacade.getCustomerClickItems(date)
    }

    @Operation(
        summary = "손님 후원 입금 확인",
        description = "통장에 실제로 입금된 후원을 해당 클릭에 표시합니다. 이미 확인된 클릭이면 금액·메모를 덮어씁니다."
    )
    @PutMapping("/donations/customer-clicks/{clickId}/deposit")
    fun confirmCustomerDonationDeposit(
        @PathVariable clickId: Long,
        @RequestBody body: ConfirmCustomerDonationDepositRequestBody
    ): CustomerDonationClickItemDto {
        return superAdminDonationFacade.confirmDeposit(clickId, body.amount, body.memo)
    }

    @Operation(summary = "손님 후원 입금 확인 취소", description = "잘못 표시한 입금 확인을 되돌려 미확인 상태로 만듭니다.")
    @DeleteMapping("/donations/customer-clicks/{clickId}/deposit")
    fun cancelCustomerDonationDeposit(
        @PathVariable clickId: Long
    ): CustomerDonationClickItemDto {
        return superAdminDonationFacade.cancelDeposit(clickId)
    }
}
