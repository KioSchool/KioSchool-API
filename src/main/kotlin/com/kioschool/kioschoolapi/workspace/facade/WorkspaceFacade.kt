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

    fun getWorkspaceAccount(workspaceId: Long): String {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val workspaceOwner = workspace.owner
        val accountUrl = workspaceOwner.accountUrl ?: ""

        val decodedBank = workspaceService.extractDecodedBank(accountUrl)
        val accountNo = workspaceService.extractAccountNo(accountUrl)

        return "$decodedBank $accountNo"
    }

    fun getWorkspaces(username: String): List<Workspace> {
        return workspaceService.getWorkspaces(username)
    }
}