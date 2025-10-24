package com.kioschool.kioschoolapi.domain.workspace.facade

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountDto
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceTableDto
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class WorkspaceFacade(
    val userService: UserService,
    val discordService: DiscordService,
    val workspaceService: WorkspaceService
) {
    fun getAllWorkspaces(name: String?, page: Int, size: Int) =
        workspaceService.getAllWorkspaces(name, page, size).map { WorkspaceDto.of(it) }

    @Cacheable(cacheNames = [CacheNames.WORKSPACES], key = "#workspaceId")
    fun getWorkspace(workspaceId: Long): WorkspaceDto {
        return WorkspaceDto.of(workspaceService.getWorkspace(workspaceId))
    }

    fun getWorkspaces(username: String): List<WorkspaceDto> {
        val user = userService.getUser(username)
        return user.getWorkspaces().map { WorkspaceDto.of(it) }
    }

    fun getWorkspaceAccount(workspaceId: Long): AccountDto? {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val workspaceOwner = workspace.owner

        return workspaceOwner.account?.let { AccountDto.of(it) }
    }

    fun createWorkspace(username: String, name: String, description: String): WorkspaceDto {
        val user = userService.getUser(username)
        workspaceService.checkCanCreateWorkspace(user)

        val workspace = workspaceService.saveNewWorkspace(user, name, description)
        workspaceService.updateWorkspaceTables(workspace)
        discordService.sendWorkspaceCreate(workspace)

        return WorkspaceDto.of(
            workspace
        )
    }

    fun inviteWorkspace(
        hostUserName: String,
        workspaceId: Long,
        userLoginId: String
    ): WorkspaceDto {
        val hostUser = userService.getUser(hostUserName)
        val workspace = workspaceService.getWorkspace(workspaceId)
        workspaceService.checkCanInviteWorkspace(hostUser, workspace)

        val user = userService.getUser(userLoginId)
        return WorkspaceDto.of(workspaceService.inviteUserToWorkspace(workspace, user))
    }

    fun joinWorkspace(username: String, workspaceId: Long): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanJoinWorkspace(user, workspace)
        workspaceService.addUserToWorkspace(workspace, user)

        return WorkspaceDto.of(
            workspace
        )
    }

    fun leaveWorkspace(username: String, workspaceId: Long): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        return WorkspaceDto.of(workspaceService.removeUserFromWorkspace(workspace, user))
    }

    fun updateTableCount(username: String, workspaceId: Long, tableCount: Int): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspaceService.updateTableCount(workspace, tableCount)
        workspaceService.updateWorkspaceTables(workspace)

        return WorkspaceDto.of(
            workspace
        )
    }

    fun updateWorkspaceInfo(
        username: String,
        workspaceId: Long,
        name: String,
        description: String,
        notice: String,
    ): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspace.name = name
        workspace.description = description
        workspace.notice = notice

        return WorkspaceDto.of(workspaceService.saveWorkspace(workspace))
    }

    fun updateWorkspaceImage(
        username: String,
        workspaceId: Long,
        imageIds: List<Long?>,
        imageFiles: List<MultipartFile>,
    ): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)

        val deleteImages = workspace.images.filter { !imageIds.contains(it.id) }
        workspaceService.deleteWorkspaceImages(workspace, deleteImages)

        return WorkspaceDto.of(workspaceService.saveWorkspaceImages(workspace, imageFiles))
    }

    fun getAllWorkspaceTables(username: String, workspaceId: Long): List<WorkspaceTableDto> {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)

        return workspaceService.getAllWorkspaceTables(workspace).map { WorkspaceTableDto.of(it) }
    }

    fun updateOrderSetting(
        username: String,
        workspaceId: Long,
        useOrderSessionTimeLimit: Boolean,
        orderSessionTimeLimitMinutes: Long
    ): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)

        workspace.workspaceSetting.useOrderSessionTimeLimit = useOrderSessionTimeLimit
        workspace.workspaceSetting.orderSessionTimeLimitMinutes = orderSessionTimeLimitMinutes

        return WorkspaceDto.of(workspaceService.saveWorkspace(workspace))
    }
}