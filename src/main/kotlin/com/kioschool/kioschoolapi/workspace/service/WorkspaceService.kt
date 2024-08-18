package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceInvitation
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceMember
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToCreateWorkspaceException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToInviteException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToJoinWorkspaceException
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.repository.WorkspaceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.net.URLDecoder

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val userService: UserService,
    private val discordService: DiscordService
) {
    fun getAllWorkspaces(name: String?, page: Int, size: Int): Page<Workspace> {
        if (name != null)
            return workspaceRepository.findByNameContains(
                name,
                PageRequest.of(page, size)
            )

        return workspaceRepository.findAll(PageRequest.of(page, size))
    }

    fun getWorkspaces(username: String): List<Workspace> {
        val user = userService.getUser(username)
        return user.getWorkspaces()
    }

    fun createWorkspace(username: String, name: String, description: String): Workspace {
        val user = userService.getUser(username)
        checkCanCreateWorkspace(user)

        val workspace = saveNewWorkspace(user, name, description)
        discordService.sendWorkspaceCreate(workspace)

        return workspace
    }

    private fun checkCanCreateWorkspace(user: User) {
        if (user.accountUrl == null) throw NoPermissionToCreateWorkspaceException()
    }

    private fun saveNewWorkspace(user: User, name: String, description: String): Workspace {
        val workspace = workspaceRepository.save(
            Workspace(
                name = name,
                owner = user,
                description = description
            )
        )
        val workspaceMember = WorkspaceMember(
            workspace = workspace,
            user = user
        )

        workspace.members.add(workspaceMember)
        return workspaceRepository.save(workspace)
    }

    fun joinWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = getWorkspace(workspaceId)

        checkCanJoinWorkspace(user, workspace)
        addUserToWorkspace(workspace, user)

        return workspace
    }

    private fun checkCanJoinWorkspace(user: User, workspace: Workspace) {
        if (workspace.invitations.none { it.user == user }) throw NoPermissionToJoinWorkspaceException()
    }

    private fun addUserToWorkspace(workspace: Workspace, user: User) {
        val workspaceMember = WorkspaceMember(
            workspace = workspace,
            user = user
        )
        workspace.members.add(workspaceMember)
        workspaceRepository.save(workspace)
    }

    fun getWorkspace(workspaceId: Long): Workspace {
        return workspaceRepository.findById(workspaceId).get()
    }

    fun getWorkspace(username: String, workspaceId: Long): Workspace {
        if (!isAccessible(username, workspaceId)) throw WorkspaceInaccessibleException()
        return getWorkspace(workspaceId)
    }

    fun isAccessible(username: String, workspace: Workspace): Boolean {
        val user = userService.getUser(username)
        return workspace.members.any { it.user.loginId == username } || user.role == UserRole.SUPER_ADMIN
    }

    fun isAccessible(username: String, workspaceId: Long): Boolean {
        return isAccessible(username, getWorkspace(workspaceId))
    }

    fun inviteWorkspace(hostUserName: String, workspaceId: Long, userLoginId: String): Workspace {
        val hostUser = userService.getUser(hostUserName)
        val workspace = getWorkspace(workspaceId)
        checkCanInviteWorkspace(hostUser, workspace)

        val user = userService.getUser(userLoginId)
        return inviteUserToWorkspace(workspace, user)
    }

    private fun checkCanInviteWorkspace(user: User, workspace: Workspace) {
        if (workspace.owner != user) throw NoPermissionToInviteException()
    }

    private fun inviteUserToWorkspace(workspace: Workspace, user: User): Workspace {
        val workspaceInvitation = WorkspaceInvitation(
            workspace = workspace,
            user = user
        )
        workspace.invitations.add(workspaceInvitation)
        return workspaceRepository.save(workspace)
    }

    fun leaveWorkspace(username: String, workspaceId: Long): Workspace {
        val user = userService.getUser(username)
        val workspace = getWorkspace(workspaceId)

        return removeUserFromWorkspace(workspace, user)
    }

    private fun removeUserFromWorkspace(workspace: Workspace, user: User): Workspace {
        workspace.members.removeIf { it.user == user }
        return workspaceRepository.save(workspace)
    }

    fun getWorkspaceAccount(workspaceId: Long): String {
        val workspace = getWorkspace(workspaceId)
        val accountUrl = workspace.owner.accountUrl ?: ""

        val decodedBank = extractDecodedBank(accountUrl)
        val accountNo = extractAccountNo(accountUrl)

        return "$decodedBank $accountNo"
    }

    private fun extractDecodedBank(accountUrl: String): String {
        val bankRegex = "bank=([^&]+)"
        val bankMatcher = Regex(bankRegex).find(accountUrl)
        val bank = bankMatcher?.groupValues?.get(1) ?: ""
        return URLDecoder.decode(bank, "UTF-8")
    }

    private fun extractAccountNo(accountUrl: String): String {
        val accountNoRegex = "accountNo=([^&]+)"
        val accountNoMatcher = Regex(accountNoRegex).find(accountUrl)
        return accountNoMatcher?.groupValues?.get(1) ?: ""
    }
}