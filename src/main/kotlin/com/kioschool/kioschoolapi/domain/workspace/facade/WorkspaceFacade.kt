package com.kioschool.kioschoolapi.domain.workspace.facade

import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.cache.annotation.WorkspaceUpdateEvent
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

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

    fun getWorkspaceAccount(workspaceId: Long): Account? {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val workspaceOwner = workspace.owner

        return workspaceOwner.account
    }

    fun createWorkspace(username: String, name: String, description: String): Workspace {
        val user = userService.getUser(username)
        workspaceService.checkCanCreateWorkspace(user)

        val workspace = workspaceService.saveNewWorkspace(user, name, description)
        workspaceService.updateWorkspaceTables(workspace)
        discordService.sendWorkspaceCreate(workspace)

        return workspace
    }

    @WorkspaceUpdateEvent
    fun inviteWorkspace(hostUserName: String, workspaceId: Long, userLoginId: String): Workspace {
        val hostUser = userService.getUser(hostUserName)
        val workspace = workspaceService.getWorkspace(workspaceId)
        workspaceService.checkCanInviteWorkspace(hostUser, workspace)

        val user = userService.getUser(userLoginId)
        return workspaceService.inviteUserToWorkspace(workspace, user)
    }

    @WorkspaceUpdateEvent
    fun joinWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanJoinWorkspace(user, workspace)
        workspaceService.addUserToWorkspace(workspace, user)

        return workspace
    }

    @WorkspaceUpdateEvent
    fun leaveWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        return workspaceService.removeUserFromWorkspace(workspace, user)
    }

    @WorkspaceUpdateEvent
    fun updateTableCount(username: String, workspaceId: Long, tableCount: Int): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspaceService.updateTableCount(workspace, tableCount)
        workspaceService.updateWorkspaceTables(workspace)

        return workspace
    }

    @WorkspaceUpdateEvent
    fun updateWorkspaceInfo(
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

    @WorkspaceUpdateEvent
    fun updateWorkspaceImage(
        username: String,
        workspaceId: Long,
        imageIds: List<Long?>,
        imageFiles: List<MultipartFile>,
    ): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)

        val deleteImages = workspace.images.filter { !imageIds.contains(it.id) }
        workspaceService.deleteWorkspaceImages(workspace, deleteImages)

        return workspaceService.saveWorkspaceImages(workspace, imageFiles)
    }

    fun getAllWorkspaceTables(username: String, workspaceId: Long): List<WorkspaceTable> {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)

        return workspaceService.getAllWorkspaceTables(workspace)
    }

    @WorkspaceUpdateEvent
    fun updateOrderSetting(
        username: String,
        workspaceId: Long,
        useOrderSessionTimeLimit: Boolean,
        orderSessionTimeLimitMinutes: Long
    ): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)

        workspace.workspaceSetting.useOrderSessionTimeLimit = useOrderSessionTimeLimit
        workspace.workspaceSetting.orderSessionTimeLimitMinutes = orderSessionTimeLimitMinutes

        return workspaceService.saveWorkspace(workspace)
    }
}