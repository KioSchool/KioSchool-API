package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.QOrder
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
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
        status: OrderStatus?,
        tableNumber: Int?
    ): List<Order> {
        val order = QOrder.order
        val query = queryFactory.selectFrom(order)
            .where(order.workspace.id.eq(workspaceId))
            .orderBy(order.createdAt.asc())

        if (startDate != null) query.where(order.createdAt.goe(startDate))
        if (endDate != null) query.where(order.createdAt.loe(endDate))
        if (status != null) query.where(order.status.eq(status))
        if (tableNumber != null) query.where(order.tableNumber.eq(tableNumber))

        return query.fetch()
    }
}