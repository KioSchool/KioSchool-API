package com.kioschool.kioschoolapi.domain.workspace.repository

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkspaceTableRepository : JpaRepository<WorkspaceTable, Long> {
    fun findByTableNumberAndWorkspace(tableNumber: Int, workspace: Workspace): WorkspaceTable
    fun findAllByWorkspaceOrderByTableNumber(workspace: Workspace): List<WorkspaceTable>
    fun countAllByWorkspace(workspace: Workspace): Long
}