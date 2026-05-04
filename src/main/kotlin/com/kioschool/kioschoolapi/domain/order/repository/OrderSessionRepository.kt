package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import org.springframework.data.jpa.repository.JpaRepository
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
}