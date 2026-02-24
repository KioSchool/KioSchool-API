package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByWorkspaceIdAndTableNumber(
        workspaceId: Long,
        tableNumber: Int,
        pageable: Pageable
    ): Page<Order>

    fun findAllByOrderSession(orderSession: OrderSession): List<Order>

    @Query("SELECT o FROM Order o WHERE o.workspace.id = :workspaceId AND o.createdAt >= :start AND o.createdAt < :end AND o.status != com.kioschool.kioschoolapi.global.common.enums.OrderStatus.CANCELLED")
    fun findValidOrders(
        @Param("workspaceId") workspaceId: Long,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<Order>
}