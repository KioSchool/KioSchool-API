package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.dto.common.WorkspaceMenuSales
import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrderProductRepository : JpaRepository<OrderProduct, Long> {
    @EntityGraph(attributePaths = ["order"])
    fun findWithOrderById(id: Long): OrderProduct?

    @Query("""
        SELECT new com.kioschool.kioschoolapi.domain.order.dto.common.WorkspaceMenuSales(
            o.workspace.id, op.productName, SUM(op.quantity)
        )
        FROM OrderProduct op
        JOIN op.order o
        WHERE o.workspace.id IN :workspaceIds
          AND o.createdAt >= :start
          AND o.createdAt < :end
          AND o.status != com.kioschool.kioschoolapi.global.common.enums.OrderStatus.CANCELLED
          AND op.productPrice > 0
        GROUP BY o.workspace.id, op.productName
    """)
    fun findMenuSales(
        @Param("workspaceIds") workspaceIds: Collection<Long>,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
    ): List<WorkspaceMenuSales>
}