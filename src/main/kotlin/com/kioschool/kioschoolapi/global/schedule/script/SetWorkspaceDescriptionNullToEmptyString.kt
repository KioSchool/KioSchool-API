package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.springframework.stereotype.Component

// workspace description null 값을 빈 문자열로 변경
@Component
class SetWorkspaceDescriptionNullToEmptyString(
    private val workspaceRepository: WorkspaceRepository
) : Runnable {
    override fun run() {
        val workspaces = workspaceRepository.findAll()
        val workspacesWithNullDescription = workspaces.filter { workspace ->
            workspace.description == null
        }

        workspacesWithNullDescription.forEach { workspace ->
            workspace.description = ""
            workspaceRepository.save(workspace)
        }
    }
}