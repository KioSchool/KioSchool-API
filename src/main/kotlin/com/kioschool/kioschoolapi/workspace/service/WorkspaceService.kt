package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceMember
import com.kioschool.kioschoolapi.workspace.repository.WorkspaceMemberRepository
import com.kioschool.kioschoolapi.workspace.repository.WorkspaceRepository
import org.springframework.stereotype.Service

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceMemberRepository: WorkspaceMemberRepository,
    private val userService: UserService
) {
    fun getWorkspaces(username: String): MutableList<Workspace> {
        val user = userService.getUser(username)
        val workspaceIds =
            workspaceMemberRepository.findAllByUserId(user.id).map { it.workspace.id }
        return workspaceRepository.findAllById(workspaceIds)
    }

    fun createWorkspace(username: String, name: String): Workspace {
        val user = userService.getUser(username)
        val workspace = Workspace(
            name = name,
            owner = user,
            members = mutableListOf()
        )
        val workspaceMember = WorkspaceMember(
            workspace = workspace,
            user = user
        )
        workspace.members.add(workspaceMember)

        workspaceMemberRepository.saveAndFlush(workspaceMember)
        workspaceRepository.saveAndFlush(workspace)

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

        workspaceMemberRepository.saveAndFlush(workspaceMember)
        workspaceRepository.saveAndFlush(workspace)

        return workspace
    }
}