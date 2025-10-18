package com.kioschool.kioschoolapi.domain.workspace.dto

import com.kioschool.kioschoolapi.domain.order.dto.OrderSessionDto
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable
import java.time.LocalDateTime

data class WorkspaceTableDto(
    val id: Long,
    val tableNumber: Int,
    val tableHash: String,
    val orderSession: OrderSessionDto?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(workspaceTable: WorkspaceTable): WorkspaceTableDto {
            return WorkspaceTableDto(
                id = workspaceTable.id,
                tableNumber = workspaceTable.tableNumber,
                tableHash = workspaceTable.tableHash,
                orderSession = workspaceTable.orderSession?.let { OrderSessionDto.of(it) },
                createdAt = workspaceTable.createdAt,
                updatedAt = workspaceTable.updatedAt
            )
        }
    }
}
