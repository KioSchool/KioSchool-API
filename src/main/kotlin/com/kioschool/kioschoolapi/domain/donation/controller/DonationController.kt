package com.kioschool.kioschoolapi.domain.donation.controller

import com.kioschool.kioschoolapi.domain.donation.dto.common.CustomerDonationClickCountResponse
import com.kioschool.kioschoolapi.domain.donation.dto.request.RecordCustomerDonationClickRequestBody
import com.kioschool.kioschoolapi.domain.donation.facade.CustomerDonationFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Donation Controller")
@RestController
class DonationController(
    private val customerDonationFacade: CustomerDonationFacade
) {
    @Operation(
        summary = "손님 후원 버튼 클릭 기록",
        description = "손님이 주문 완료 화면 후원 모달의 송금 버튼을 누를 때 호출합니다. 로그인 불필요. 오늘 누적 클릭 수를 반환합니다."
    )
    @PostMapping("/donations/customer-clicks")
    @ResponseStatus(HttpStatus.CREATED)
    fun recordCustomerDonationClick(
        @RequestBody body: RecordCustomerDonationClickRequestBody
    ): CustomerDonationClickCountResponse {
        return customerDonationFacade.recordClick(body)
    }

    @Operation(
        summary = "오늘 손님 후원 클릭 수 조회",
        description = "KST 자정 이후 기록된 손님 후원 버튼 클릭 수. 후원 모달에서 '오늘 N명이 함께했어요' 표시에 사용합니다."
    )
    @GetMapping("/donations/customer-clicks/today-count")
    fun getTodayCustomerDonationClickCount(): CustomerDonationClickCountResponse {
        return customerDonationFacade.getTodayCount()
    }
}
