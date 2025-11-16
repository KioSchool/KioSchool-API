package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.domain.order.entity.QOrderSession
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CustomOrderSessionRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findAllByCondition(
        workspaceId: Long,
        tableNumber: Int?,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<OrderSession> {
        val orderSession = QOrderSession.orderSession
        val query = queryFactory.selectFrom(orderSession)
            .where(orderSession.workspace.id.eq(workspaceId))
            .where(orderSession.createdAt.between(start, end))
            .orderBy(orderSession.createdAt.asc())

        if (tableNumber != null) {
            query.where(orderSession.tableNumber.eq(tableNumber))
        }

        return query.fetch()
    }
}
