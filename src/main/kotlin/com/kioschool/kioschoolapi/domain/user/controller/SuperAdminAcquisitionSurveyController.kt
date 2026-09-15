package com.kioschool.kioschoolapi.domain.user.controller

import com.kioschool.kioschoolapi.domain.user.dto.common.AcquisitionSurveyResponseDto
import com.kioschool.kioschoolapi.domain.user.dto.common.AcquisitionSurveySummaryDto
import com.kioschool.kioschoolapi.domain.user.facade.SuperAdminAcquisitionSurveyFacade
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Super Admin Acquisition Survey Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminAcquisitionSurveyController(
    private val superAdminAcquisitionSurveyFacade: SuperAdminAcquisitionSurveyFacade
) {
    @Operation(
        summary = "유입 경로 설문 요약",
        description = "전체 기간의 응답/건너뜀/미응답 수와 유입 경로별 분포, 학교별 분포를 조회합니다."
    )
    @GetMapping("/users/acquisition-survey")
    fun getAcquisitionSurveySummary(): AcquisitionSurveySummaryDto {
        return superAdminAcquisitionSurveyFacade.getSummary()
    }

    @Operation(
        summary = "유입 경로 설문 응답 목록",
        description = "개별 응답을 최신순으로 조회합니다. channel·school을 주면 해당 유입 경로·학교만 조회합니다."
    )
    @GetMapping("/users/acquisition-survey/responses")
    fun getAcquisitionSurveyResponses(
        @RequestParam(required = false) channel: AcquisitionChannel?,
        @RequestParam(required = false) school: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<AcquisitionSurveyResponseDto> {
        return superAdminAcquisitionSurveyFacade.getResponses(channel, school, page, size)
    }
}
