package com.kioschool.kioschoolapi.global.common.schedule.script

import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.common.schedule.Runnable
import org.springframework.stereotype.Component

@Component
class InitWorkspaceSettings(
    private val workspaceService: WorkspaceService
) : Runnable {
    override fun run() {
        val workspaces = workspaceService.getAllWorkspaces(null, 0, 1000)
        workspaces.forEach { workspace ->
            workspace.workspaceSetting = WorkspaceSetting()
            workspaceService.saveWorkspace(workspace)
        }
    }
}