package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.common.annotation.AdminUsername
import com.kioschool.kioschoolapi.order.dto.*
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.order.facade.OrderFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
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
        @RequestParam("status") status: String? = null,
        @RequestParam("tableNumber") tableNumber: Int? = null
    ): List<Order> {
        return orderFacade.getOrdersByCondition(
            username,
            workspaceId,
            startDate,
            endDate,
            status,
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
        @RequestParam("tableNumber") tableNumber: Int,
        @RequestParam("page") page: Int,
        @RequestParam("size") size: Int
    ): Page<Order> {
        return orderFacade.getOrdersByTable(
            username,
            workspaceId,
            tableNumber,
            page,
            size
        )
    }

    @Operation(summary = "실시간 주문 조회", description = "실시간 주문을 조회합니다.")
    @GetMapping("/orders/realtime")
    fun getRealtimeOrders(
        @AdminUsername username: String,
        @RequestParam("workspaceId") workspaceId: Long
    ): List<Order> {
        return orderFacade.getRealtimeOrders(username, workspaceId)
    }

    @Operation(summary = "주문 상태 변경", description = "주문을 상태를 변경합니다.")
    @PostMapping("/order/status")
    fun changeOrderStatus(
        @AdminUsername username: String,
        @RequestBody body: ChangeOrderStatusRequestBody
    ): Order {
        return orderFacade.changeOrderStatus(
            username,
            body.workspaceId,
            body.orderId,
            body.status
        )
    }

    @Operation(summary = "주문 별 상품 서빙 완료", description = "주문 별 상품의 서빙 상태를 변경합니다.")
    @PostMapping("/order/product")
    fun serveOrderProduct(
        @AdminUsername username: String,
        @RequestBody body: ServeOrderProductRequestBody
    ): OrderProduct {
        return orderFacade.serveOrderProduct(
            username,
            body.workspaceId,
            body.orderProductId,
            body.isServed
        )
    }

    @Operation(summary = "주문 별 상품 서빙 개수 변경", description = "주문 별 상품의 서빙 개수를 변경합니다.")
    @PutMapping("/order/product")
    fun changeOrderProductServedCount(
        @AdminUsername username: String,
        @RequestBody body: ChangeOrderProductServedCount
    ): OrderProduct {
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

}