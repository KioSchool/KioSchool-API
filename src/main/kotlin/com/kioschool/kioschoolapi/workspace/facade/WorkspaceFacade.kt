package com.kioschool.kioschoolapi.workspace.facade

import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.stereotype.Component

@Component
class WorkspaceFacade(
    val userService: UserService,
    val discordService: DiscordService,
    val workspaceService: WorkspaceService
) {
    fun getWorkspace(workspaceId: Long): Workspace {
        return workspaceService.getWorkspace(workspaceId)
    }

    fun getWorkspaces(username: String): List<Workspace> {
        val user = userService.getUser(username)
        return user.getWorkspaces()
    }

    fun getWorkspaceAccount(workspaceId: Long): String {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val workspaceOwner = workspace.owner
        val accountUrl = workspaceOwner.accountUrl ?: ""

        val decodedBank = workspaceService.extractDecodedBank(accountUrl)
        val accountNo = workspaceService.extractAccountNo(accountUrl)

        return "$decodedBank $accountNo"
    }

    fun createWorkspace(username: String, name: String, description: String): Workspace {
        val user = userService.getUser(username)
        workspaceService.checkCanCreateWorkspace(user)

        val workspace = workspaceService.saveNewWorkspace(user, name, description)
        discordService.sendWorkspaceCreate(workspace)

        return workspace
    }

    fun inviteWorkspace(hostUserName: String, workspaceId: Long, userLoginId: String): Workspace {
        val hostUser = userService.getUser(hostUserName)
        val workspace = workspaceService.getWorkspace(workspaceId)
        workspaceService.checkCanInviteWorkspace(hostUser, workspace)

        val user = userService.getUser(userLoginId)
        return workspaceService.inviteUserToWorkspace(workspace, user)
    }

    fun joinWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanJoinWorkspace(user, workspace)
        workspaceService.addUserToWorkspace(workspace, user)

        return workspace
    }

    fun leaveWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        return workspaceService.removeUserFromWorkspace(workspace, user)
    }

    fun updateTableCount(username: String, workspaceId: Long, tableCount: Int): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspaceService.updateTableCount(workspace, tableCount)

        return workspace
    }

    fun updateWorkspace(
        username: String,
        workspaceId: Long,
        name: String,
        description: String,
        notice: String,
    ): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspace.name = name
        workspace.description = description
        workspace.notice = notice

        return workspaceService.saveWorkspace(workspace)
    }
}