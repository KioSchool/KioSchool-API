package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.repository.WorkspaceRepository
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val userService: UserService
) {
    fun getWorkspaces(username: String): MutableList<Workspace> {
        val user = userService.getUser(username)
        return user.workspaces
    }

    fun createWorkspace(username: String, name: String): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceRepository.save(
            Workspace(
                name = name,
                owner = user,
                users = mutableListOf()
            )
        )

        workspace.users.add(user)
        workspaceRepository.save(workspace)

        return workspace
    }

    fun joinWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceRepository.findById(workspaceId).get()
        workspace.users.add(user)
        workspaceRepository.save(workspace)

        return workspace
    }
}