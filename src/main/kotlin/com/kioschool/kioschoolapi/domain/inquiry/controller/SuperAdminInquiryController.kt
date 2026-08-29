package com.kioschool.kioschoolapi.domain.inquiry.controller

import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquiryDetailDto
import com.kioschool.kioschoolapi.domain.inquiry.dto.common.InquirySummaryDto
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import com.kioschool.kioschoolapi.domain.inquiry.facade.InquiryFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Inquiry Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminInquiryController(
    private val inquiryFacade: InquiryFacade
) {
    @Operation(summary = "문의 목록 조회", description = "접수된 문의를 최신순으로 조회합니다.")
    @GetMapping("/inquiries")
    fun getInquiries(
        @RequestParam(required = false) status: InquiryStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<InquirySummaryDto> {
        return inquiryFacade.getInquiries(status, page, size)
    }

    @Operation(summary = "문의 상세 조회", description = "문의 본문과 첨부 이미지를 조회합니다.")
    @GetMapping("/inquiries/{inquiryId}")
    fun getInquiry(
        @PathVariable inquiryId: Long,
    ): InquiryDetailDto {
        return inquiryFacade.getInquiry(inquiryId)
    }
}
