package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OrderSessionRepository : JpaRepository<OrderSession, Long> {
    fun findAllByEndAtIsNull(): List<OrderSession>
    fun findAllByWorkspaceAndEndAtIsNotNull(workspace: Workspace): List<OrderSession>

    fun findAllByWorkspaceIdAndCreatedAtBetween(
        workspaceId: Long,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<OrderSession>

    fun findAllByWorkspaceId(workspaceId: Long): List<OrderSession>

    @Modifying
    @Query("UPDATE OrderSession s SET s.customerName = :masked WHERE s.createdAt < :cutoff AND s.customerName IS NOT NULL AND s.customerName <> :masked")
    fun maskCustomerNamesCreatedBefore(
        @Param("cutoff") cutoff: LocalDateTime,
        @Param("masked") masked: String
    ): Int
}
