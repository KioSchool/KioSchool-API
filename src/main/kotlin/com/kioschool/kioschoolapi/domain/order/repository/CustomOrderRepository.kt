package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.dashboard.dto.ProductIdQuantityDto
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.QOrder
import com.kioschool.kioschoolapi.domain.order.entity.QOrderProduct
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.querydsl.core.types.Projections.constructor
import com.querydsl.core.types.dsl.Expressions.numberTemplate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CustomOrderRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findAllByCondition(
        workspaceId: Long,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        statuses: List<OrderStatus>?,
        tableNumber: Int?
    ): List<Order> {
        val order = QOrder.order
        val query = queryFactory.selectFrom(order)
            .where(order.workspace.id.eq(workspaceId))
            .orderBy(order.createdAt.asc())

        if (startDate != null) query.where(order.createdAt.goe(startDate))
        if (endDate != null) query.where(order.createdAt.loe(endDate))
        if (statuses != null && statuses.isNotEmpty()) query.where(order.status.`in`(statuses))
        if (tableNumber != null) query.where(order.tableNumber.eq(tableNumber))

        return query.fetch()
    }

    fun findAllByOrderSessionIds(sessionIds: List<Long>): List<Order> {
        val order = QOrder.order
        return queryFactory.selectFrom(order)
            .where(order.orderSession.id.`in`(sessionIds))
            .fetch()
    }

    fun getSalesSum(workspaceId: Long, start: LocalDateTime, end: LocalDateTime): Long {
        val order = QOrder.order
        // Workaround for Kotlin QueryDSL "invisible sum" issue
        val priceSum = numberTemplate(Long::class.java, "sum({0})", order.totalPrice)
        return queryFactory.select(priceSum)
            .from(order)
            .where(
                order.workspace.id.eq(workspaceId),
                order.createdAt.between(start, end),
                order.status.ne(OrderStatus.CANCELLED)
            )
            .fetchOne() ?: 0L
    }

    fun getOrderCount(workspaceId: Long, start: LocalDateTime, end: LocalDateTime): Long {
        val order = QOrder.order
        return queryFactory.select(order.count())
            .from(order)
            .where(
                order.workspace.id.eq(workspaceId),
                order.createdAt.between(start, end),
                order.status.ne(OrderStatus.CANCELLED)
            )
            .fetchOne() ?: 0L
    }

    fun getTopSellingProducts(
        workspaceId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
        limit: Int
    ): List<ProductIdQuantityDto> {
        val order = QOrder.order
        val orderProduct = QOrderProduct.orderProduct

        // Workaround for Kotlin QueryDSL "invisible sum" issue
        val quantitySum = numberTemplate(
            Long::class.java,
            "sum({0})",
            orderProduct.quantity
        )

        return queryFactory
            .select(
                constructor(
                    ProductIdQuantityDto::class.java,
                    orderProduct.productId,
                    orderProduct.productName,
                    quantitySum
                )
            )
            .from(orderProduct)
            .join(orderProduct.order, order)
            .where(
                order.workspace.id.eq(workspaceId),
                order.createdAt.between(start, end),
                order.status.ne(OrderStatus.CANCELLED)
            )
            .groupBy(orderProduct.productId, orderProduct.productName)
            .orderBy(quantitySum.desc())
            .limit(limit.toLong())
            .fetch()
    }
}