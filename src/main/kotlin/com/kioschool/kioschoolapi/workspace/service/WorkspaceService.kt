package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceMember
import com.kioschool.kioschoolapi.workspace.repository.WorkspaceRepository
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val userService: UserService
) {
    fun getWorkspaces(username: String): List<Workspace> {
        val user = userService.getUser(username)
        return user.getWorkspaces()
    }

    fun createWorkspace(username: String, name: String): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceRepository.save(
            Workspace(
                name = name,
                owner = user,
            )
        )
        val workspaceMember = WorkspaceMember(
            workspace = workspace,
            user = user
        )

        workspace.members.add(workspaceMember)
        workspaceRepository.save(workspace)

        return workspace
    }

    fun joinWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceRepository.findById(workspaceId).get()
        val workspaceMember = WorkspaceMember(
            workspace = workspace,
            user = user
        )

        workspace.members.add(workspaceMember)
        workspaceRepository.save(workspace)

        return workspace
    }

    fun getWorkspace(workspaceId: Long): Workspace {
        return workspaceRepository.findById(workspaceId).get()
    }
}