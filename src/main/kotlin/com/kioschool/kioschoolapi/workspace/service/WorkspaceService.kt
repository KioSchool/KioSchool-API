package com.kioschool.kioschoolapi.workspace.service

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceInvitation
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceMember
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToCreateWorkspaceException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToInviteException
import com.kioschool.kioschoolapi.workspace.exception.NoPermissionToJoinWorkspaceException
import com.kioschool.kioschoolapi.workspace.repository.WorkspaceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.net.URLDecoder

@Service
class WorkspaceService(
    val workspaceRepository: WorkspaceRepository,
    val userService: UserService,
) {
    fun getAllWorkspaces(name: String?, page: Int, size: Int): Page<Workspace> {
        if (name != null)
            return workspaceRepository.findByNameContains(
                name,
                PageRequest.of(page, size)
            )

        return workspaceRepository.findAll(PageRequest.of(page, size))
    }

    fun checkCanCreateWorkspace(user: User) {
        if (user.accountUrl == null) throw NoPermissionToCreateWorkspaceException()
    }

    fun saveNewWorkspace(user: User, name: String, description: String): Workspace {
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

    fun checkCanJoinWorkspace(user: User, workspace: Workspace) {
        if (workspace.invitations.none { it.user == user }) throw NoPermissionToJoinWorkspaceException()
    }

    fun addUserToWorkspace(workspace: Workspace, user: User) {
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

    fun isAccessible(username: String, workspaceId: Long): Boolean {
        val workspace = getWorkspace(workspaceId)
        val user = userService.getUser(username)

        return workspace.members.any { it.user.loginId == username } || user.role == UserRole.SUPER_ADMIN
    }

    fun checkCanInviteWorkspace(user: User, workspace: Workspace) {
        if (workspace.owner != user) throw NoPermissionToInviteException()
    }

    fun inviteUserToWorkspace(workspace: Workspace, user: User): Workspace {
        val workspaceInvitation = WorkspaceInvitation(
            workspace = workspace,
            user = user
        )
        workspace.invitations.add(workspaceInvitation)
        return workspaceRepository.save(workspace)
    }

    fun removeUserFromWorkspace(workspace: Workspace, user: User): Workspace {
        workspace.members.removeIf { it.user == user }
        return workspaceRepository.save(workspace)
    }

    fun extractDecodedBank(accountUrl: String): String {
        val bankRegex = "bank=([^&]+)"
        val bankMatcher = Regex(bankRegex).find(accountUrl)
        val bank = bankMatcher?.groupValues?.get(1) ?: ""
        return URLDecoder.decode(bank, "UTF-8")
    }

    fun extractAccountNo(accountUrl: String): String {
        val accountNoRegex = "accountNo=([^&]+)"
        val accountNoMatcher = Regex(accountNoRegex).find(accountUrl)
        return accountNoMatcher?.groupValues?.get(1) ?: ""
    }
}