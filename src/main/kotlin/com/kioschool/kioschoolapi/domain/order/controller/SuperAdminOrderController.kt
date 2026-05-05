package com.kioschool.kioschoolapi.domain.order.controller

import com.kioschool.kioschoolapi.domain.order.dto.common.SuperAdminOrderDto
import com.kioschool.kioschoolapi.domain.order.facade.OrderFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Tag(name = "Super Admin Order Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminOrderController(
    private val orderFacade: OrderFacade
) {
    @Operation(
        summary = "전체 주문 모니터링",
        description = """서비스 전체 주문을 조회합니다. 워크스페이스 정보가 함께 포함됩니다.
            - workspaceId: 특정 워크스페이스 필터 (미입력 시 전체 조회)
            - statuses: NOT_PAID, PAID, SERVED, CANCELLED 중 복수 선택 가능
            - 최신 주문부터 내림차순 정렬"""
    )
    @GetMapping("/orders")
    fun getAllOrders(
        @RequestParam(required = false) workspaceId: Long?,
        @RequestParam(required = false) startDate: LocalDateTime?,
        @RequestParam(required = false) endDate: LocalDateTime?,
        @RequestParam(required = false) statuses: List<String>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<SuperAdminOrderDto> {
        return orderFacade.getAllOrdersGlobal(workspaceId, startDate, endDate, statuses, page, size)
    }
}
