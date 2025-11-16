package com.kioschool.kioschoolapi.domain.order.controller

import com.kioschool.kioschoolapi.domain.order.dto.common.OrderDto
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderHourlyPrice
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderPrefixSumPrice
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderProductDto
import com.kioschool.kioschoolapi.domain.order.dto.common.OrderSessionDto
import com.kioschool.kioschoolapi.domain.order.dto.request.*
import com.kioschool.kioschoolapi.domain.order.facade.OrderFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@Tag(name = "Admin Order Controller")
@RestController
@RequestMapping("/admin")
class AdminOrderController(
    private val orderFacade: OrderFacade
) {

    @Operation(summary = "주문 조회", description = "주문을 조회합니다. 조건을 입력하지 않으면 모든 주문을 조회합니다.")
    @GetMapping("/orders")
    fun getOrdersByCondition(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("startDate") startDate: LocalDateTime? = null,
        @RequestParam("endDate") endDate: LocalDateTime? = null,
        @RequestParam("statuses") statuses: List<String>? = null,
        @RequestParam("tableNumber") tableNumber: Int? = null
    ): List<OrderDto> {
        return orderFacade.getOrdersByCondition(
            username,
            workspaceId,
            startDate,
            endDate,
            statuses,
            tableNumber
        )
    }

    @Operation(summary = "주문 누적 총액 조회", description = "주문 누적 총액을 조회합니다.")
    @GetMapping("/orders/prefix-sum/price")
    fun getOrderPricePrefixSum(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("startDate") startDate: LocalDateTime,
        @RequestParam("endDate") endDate: LocalDateTime,
        @RequestParam("status") status: String? = null,
    ): List<OrderPrefixSumPrice> {
        return orderFacade.getOrderPricePrefixSum(
            username,
            workspaceId,
            startDate,
            endDate,
            status
        )
    }

    @Operation(summary = "주문 시간대별 매출 조회", description = "주문 시간대별 매출을 조회합니다.")
    @GetMapping("/orders/hourly/price")
    fun getOrderHourlyPrice(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("startDate") startDate: LocalDateTime,
        @RequestParam("endDate") endDate: LocalDateTime,
        @RequestParam("status") status: String? = null
    ): List<OrderHourlyPrice> {
        return orderFacade.getOrderHourlyPrice(
            username,
            workspaceId,
            startDate,
            endDate,
            status
        )
    }

    @Operation(summary = "테이블별 주문 조회", description = "테이블별 주문을 조회합니다.")
    @GetMapping("/orders/table")
    fun getOrdersByTable(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("tableNumber") tableNumber: Int? = null,
        @RequestParam("startDate") startDate: LocalDateTime,
        @RequestParam("endDate") endDate: LocalDateTime
    ): List<OrderSessionDto> {
        return orderFacade.getOrdersByTable(
            username,
            workspaceId,
            tableNumber,
            startDate,
            endDate
        )
    }

    @Operation(summary = "실시간 주문 조회", description = "실시간 주문을 조회합니다.")
    @GetMapping("/orders/realtime")
    fun getRealtimeOrders(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long
    ): List<OrderDto> {
        return orderFacade.getRealtimeOrders(username, workspaceId)
    }

    @Operation(summary = "주문 상태 변경", description = "주문을 상태를 변경합니다.")
    @PostMapping("/order/status")
    fun changeOrderStatus(
        @AdminUsername username: String,
        @RequestBody body: ChangeOrderStatusRequestBody
    ): OrderDto {
        return orderFacade.changeOrderStatus(
            username,
            body.workspaceId,
            body.orderId,
            body.status
        )
    }

    @Operation(summary = "주문 별 상품 서빙 개수 변경", description = "주문 별 상품의 서빙 개수를 변경합니다.")
    @PutMapping("/order/product")
    fun changeOrderProductServedCount(
        @AdminUsername username: String,
        @RequestBody body: ChangeOrderProductServedCount
    ): OrderProductDto {
        return orderFacade.changeOrderProductServedCount(
            username,
            body.workspaceId,
            body.orderProductId,
            body.servedCount
        )
    }

    @Operation(summary = "주문 번호 초기화", description = "주문 번호를 초기화합니다.")
    @PostMapping("/order/number/reset")
    fun resetOrderNumber(
        @AdminUsername username: String,
        @RequestBody body: ResetOrderNumberRequestBody
    ) {
        orderFacade.resetOrderNumber(username, body.workspaceId)
    }

    @Operation(summary = "주문 세션 주문 조회", description = "주문 세션에 저장된 주문을 조회합니다.")
    @GetMapping("/order/session")
    fun getOrdersByOrderSession(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("orderSessionId") orderSessionId: Long
    ): List<OrderDto> {
        return orderFacade.getOrdersByOrderSession(username, workspaceId, orderSessionId)
    }

    @Operation(summary = "주문 세션 시작", description = "주문 세션을 시작합니다.")
    @PostMapping("/order/session/start")
    fun startOrderSession(
        @AdminUsername username: String,
        @RequestBody body: StartOrderSessionRequestBody
    ): OrderSessionDto {
        return orderFacade.startOrderSession(
            username,
            body.workspaceId,
            body.tableNumber
        )
    }

    @Operation(summary = "주문 세션 예상 종료 시간 변경", description = "주문 세션의 예상 종료 시간을 변경합니다.")
    @PatchMapping("/order/session")
    fun updateExpectedEndTime(
        @AdminUsername username: String,
        @RequestBody body: UpdateExpectedEndAtRequestBody
    ): OrderSessionDto {
        return orderFacade.updateOrderSessionExpectedEndAt(
            username,
            body.workspaceId,
            body.orderSessionId,
            body.expectedEndAt
        )
    }

    @Operation(summary = "주문 세션 종료", description = "주문 세션을 종료합니다.")
    @PostMapping("/order/session/end")
    fun endOrderSession(
        @AdminUsername username: String,
        @RequestBody body: EndOrderSessionRequestBody
    ): OrderSessionDto {
        return orderFacade.endOrderSession(
            username,
            body.workspaceId,
            body.tableNumber,
            body.orderSessionId
        )
    }
}