package com.kioschool.kioschoolapi.domain.inquiry.controller

import com.kioschool.kioschoolapi.domain.inquiry.dto.common.CreateInquiryResponse
import com.kioschool.kioschoolapi.domain.inquiry.dto.request.CreateInquiryRequestBody
import com.kioschool.kioschoolapi.domain.inquiry.facade.InquiryFacade
import com.kioschool.kioschoolapi.global.turnstile.service.TurnstileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Inquiry Controller")
@RestController
class InquiryController(
    private val inquiryFacade: InquiryFacade,
    private val turnstileService: TurnstileService,
) {
    @Operation(summary = "문의 접수", description = "로그인하지 않은 사용자도 문의를 접수할 수 있습니다.")
    @PostMapping("/inquiries", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createInquiry(
        @RequestPart("body") @Valid body: CreateInquiryRequestBody,
        @RequestPart(required = false) imageFiles: List<MultipartFile>?,
    ): CreateInquiryResponse {
        // 외부 HTTP 호출이라 DB 커넥션을 잡는 파사드 트랜잭션에 들어가기 전에 검증한다
        turnstileService.verify(body.captchaToken)
        return inquiryFacade.createInquiry(body, imageFiles)
    }
}
