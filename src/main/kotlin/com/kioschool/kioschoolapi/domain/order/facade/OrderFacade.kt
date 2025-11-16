package com.kioschool.kioschoolapi.domain.order.facade

import com.kioschool.kioschoolapi.domain.order.dto.common.*
import com.kioschool.kioschoolapi.domain.order.dto.request.OrderProductRequestBody
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import com.kioschool.kioschoolapi.domain.order.exception.NoOrderSessionException
import com.kioschool.kioschoolapi.domain.order.exception.OrderSessionAlreadyExistException
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.WebsocketType
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val workspaceService: WorkspaceService,
    private val productService: ProductService
) {
    @Transactional
    fun createOrder(
        workspaceId: Long,
        tableNumber: Int,
        customerName: String,
        rawOrderProducts: List<OrderProductRequestBody>
    ): OrderDto {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val productIds = rawOrderProducts.map { it.productId }
        val table = workspaceService.getWorkspaceTable(workspace, tableNumber)
        val currentOrderSession = table.orderSession ?: throw NoOrderSessionException()

        productService.validateProducts(workspaceId, productIds)

        val orderNumber = orderService.getOrderNumber(workspaceId)
        val order = orderService.saveOrder(
            Order(
                workspace = workspace,
                tableNumber = tableNumber,
                customerName = customerName,
                orderNumber = orderNumber,
                orderSession = currentOrderSession,
            )
        )

        val products = productService.getAllProductsByCondition(workspaceId)
        val productMap = products.associateBy { it.id }
        val orderProducts = rawOrderProducts.filter { productMap.containsKey(it.productId) }.map {
            val product = productMap[it.productId]!!
            OrderProduct(
                order = order,
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = it.quantity,
                totalPrice = product.price * it.quantity
            )
        }

        order.orderProducts.addAll(orderProducts)
        order.totalPrice = orderProducts.sumOf { it.totalPrice }
        return OrderDto.of(
            orderService.saveOrderAndSendWebsocketMessage(
                order,
                WebsocketType.CREATED
            )
        )
    }

    @Cacheable(cacheNames = [CacheNames.ORDERS], key = "#orderId")
    fun getOrder(orderId: Long): OrderDto {
        return OrderDto.of(orderService.getOrder(orderId))
    }

    fun getOrdersByCondition(
        username: String,
        workspaceId: Long,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        statuses: List<String>?,
        tableNumber: Int?
    ): List<OrderDto> {
        workspaceService.checkAccessible(username, workspaceId)

        val parsedStatuses = statuses?.map { OrderStatus.valueOf(it) }

        return orderService.getAllOrdersByCondition(
            workspaceId,
            startDate,
            endDate,
            parsedStatuses,
            tableNumber
        ).map { OrderDto.of(it) }
    }

    fun getRealtimeOrders(username: String, workspaceId: Long): List<OrderDto> {
        workspaceService.checkAccessible(username, workspaceId)

        val startDate = LocalDateTime.now().minusHours(2)
        val endDate = LocalDateTime.now()

        return orderService.getAllOrdersByCondition(workspaceId, startDate, endDate, null, null)
            .map { OrderDto.of(it) }
    }

    fun changeOrderStatus(
        username: String,
        workspaceId: Long,
        orderId: Long,
        status: String
    ): OrderDto {
        workspaceService.checkAccessible(username, workspaceId)

        val order = orderService.getOrder(orderId)
        order.status = OrderStatus.valueOf(status)

        return OrderDto.of(
            orderService.saveOrderAndSendWebsocketMessage(
                order,
                WebsocketType.UPDATED
            )
        )
    }

    fun getOrdersByTable(
        username: String,
        workspaceId: Long,
        tableNumber: Int?,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<OrderSessionDto> {
        workspaceService.checkAccessible(username, workspaceId)
        return orderService.getAllOrderSessionsByCondition(
            workspaceId,
            tableNumber,
            startDate,
            endDate
        )
            .map { OrderSessionDto.of(it) }
    }

    fun changeOrderProductServedCount(
        username: String,
        workspaceId: Long,
        orderProductId: Long,
        servedCount: Int
    ): OrderProductDto {
        workspaceService.checkAccessible(username, workspaceId)

        val orderProduct = orderService.getOrderProduct(orderProductId)
        orderProduct.servedCount = servedCount
        orderProduct.isServed = orderProduct.servedCount == orderProduct.quantity
        return OrderProductDto.of(orderService.saveOrderProductAndSendWebsocketMessage(orderProduct))
    }

    fun resetOrderNumber(username: String, workspaceId: Long) {
        workspaceService.checkAccessible(username, workspaceId)

        orderService.resetOrderNumber(workspaceId)
    }

    fun getOrderPricePrefixSum(
        username: String,
        workspaceId: Long,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        status: String?
    ): List<OrderPrefixSumPrice> {
        workspaceService.checkAccessible(username, workspaceId)

        val parsedStatuses = status?.let { listOf(OrderStatus.valueOf(it)) }

        val orders = orderService.getAllOrdersByCondition(
            workspaceId,
            startDate,
            endDate,
            parsedStatuses,
            null
        )

        val grouped = orders
            .groupBy { it.createdAt!!.truncatedTo(ChronoUnit.HOURS) }
            .toSortedMap()

        var sum = 0L
        return grouped.map { (time, ordersAtTime) ->
            sum += ordersAtTime.sumOf { it.totalPrice }
            OrderPrefixSumPrice(time, sum)
        }
    }

    fun getOrderHourlyPrice(
        username: String,
        workspaceId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        status: String?
    ): List<OrderHourlyPrice> {
        workspaceService.checkAccessible(username, workspaceId)

        val parsedStatuses = status?.let { listOf(OrderStatus.valueOf(it)) }

        val orders = orderService.getAllOrdersByCondition(
            workspaceId,
            startDate,
            endDate,
            parsedStatuses,
            null
        )

        val grouped = orders
            .groupBy { it.createdAt!!.truncatedTo(ChronoUnit.HOURS) }
            .toSortedMap()

        return grouped.map { (time, ordersAtTime) ->
            OrderHourlyPrice(time, ordersAtTime.sumOf { it.totalPrice }.toLong())
        }
    }

    @Transactional
    fun startOrderSession(
        username: String,
        workspaceId: Long,
        tableNumber: Int
    ): OrderSessionDto {
        workspaceService.checkAccessible(username, workspaceId)

        val workspace = workspaceService.getWorkspace(workspaceId)
        val setting = workspace.workspaceSetting
        val table = workspaceService.getWorkspaceTable(workspace, tableNumber)

        if (table.orderSession != null) throw OrderSessionAlreadyExistException()

        val orderSession = orderService.createOrderSession(workspace, table, setting)
        table.orderSession = orderSession
        workspaceService.saveWorkspaceTable(table)

        return OrderSessionDto.of(
            orderSession
        )
    }

    @Transactional
    fun updateOrderSessionExpectedEndAt(
        username: String,
        workspaceId: Long,
        orderSessionId: Long,
        expectedEndAt: LocalDateTime,
    ): OrderSessionDto {
        workspaceService.checkAccessible(username, workspaceId)

        val orderSession = orderService.getOrderSession(orderSessionId)
        workspaceService.checkAccessible(username, orderSession.workspace.id)
        orderSession.expectedEndAt = expectedEndAt

        return OrderSessionDto.of(orderService.saveOrderSession(orderSession))
    }

    @Transactional
    fun endOrderSession(
        username: String,
        workspaceId: Long,
        tableNumber: Int,
        orderSessionId: Long
    ): OrderSessionDto {
        workspaceService.checkAccessible(username, workspaceId)

        val table = workspaceService.getWorkspaceTable(
            workspaceService.getWorkspace(workspaceId),
            tableNumber
        )
        table.orderSession = null
        workspaceService.saveWorkspaceTable(table)

        val orderSession = orderService.getOrderSession(orderSessionId)
        orderSession.endAt = LocalDateTime.now()
        return OrderSessionDto.of(orderService.saveOrderSession(orderSession))
    }

    fun getOrdersByOrderSession(
        username: String,
        workspaceId: Long,
        orderSessionId: Long
    ): List<OrderDto> {
        workspaceService.checkAccessible(username, workspaceId)

        val orderSession = orderService.getOrderSession(orderSessionId)

        workspaceService.checkAccessible(username, orderSession.workspace.id)

        return orderService.getAllOrdersByOrderSession(orderSession).map { OrderDto.of(it) }
    }

    fun isOrderAvailable(workspaceId: Long, tableNumber: Int): Boolean {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val table = workspaceService.getWorkspaceTable(workspace, tableNumber)

        val isOrderSessionStarted = table.orderSession != null
        return isOrderSessionStarted
    }
}