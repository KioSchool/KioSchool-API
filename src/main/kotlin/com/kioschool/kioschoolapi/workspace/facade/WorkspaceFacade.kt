package com.kioschool.kioschoolapi.workspace.facade

import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
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
        imageUrl1: String?,
        imageUrl2: String?,
        imageUrl3: String?,
        imageFile1: MultipartFile?,
        imageFile2: MultipartFile?,
        imageFile3: MultipartFile?
    ): Workspace {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspace.name = name
        workspace.description = description
        workspace.notice = notice

        if (workspace.imageUrl1 != imageUrl1) {
            workspace.imageUrl1 =
                workspaceService.getImageUrl(workspaceId, workspace.id, imageFile1)
        }

        if (workspace.imageUrl2 != imageUrl2) {
            workspace.imageUrl2 =
                workspaceService.getImageUrl(workspaceId, workspace.id, imageFile2)
        }

        if (workspace.imageUrl3 != imageUrl3) {
            workspace.imageUrl3 =
                workspaceService.getImageUrl(workspaceId, workspace.id, imageFile3)
        }

        return workspaceService.saveWorkspace(workspace)
    }
}