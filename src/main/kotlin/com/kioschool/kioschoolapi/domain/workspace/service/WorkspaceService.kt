package com.kioschool.kioschoolapi.domain.workspace.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.entity.*
import com.kioschool.kioschoolapi.domain.workspace.exception.NoPermissionToCreateWorkspaceException
import com.kioschool.kioschoolapi.domain.workspace.exception.NoPermissionToInviteException
import com.kioschool.kioschoolapi.domain.workspace.exception.NoPermissionToJoinWorkspaceException
import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceTableRepository
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class WorkspaceService(
    @Value("\${cloud.aws.s3.default-path}")
    private val workspacePath: String,
    val workspaceRepository: WorkspaceRepository,
    val workspaceTableRepository: WorkspaceTableRepository,
    val userService: UserService,
    val s3Service: S3Service
) {
    fun getAllWorkspaces(name: String?, page: Int, size: Int): Page<Workspace> {
        if (!name.isNullOrBlank())
            return workspaceRepository.findByNameContains(
                name,
                PageRequest.of(page, size)
            )

        return workspaceRepository.findAll(PageRequest.of(page, size))
    }

    fun checkCanCreateWorkspace(user: User) {
        if (user.account == null) throw NoPermissionToCreateWorkspaceException()
    }

    fun saveNewWorkspace(user: User, name: String, description: String): Workspace {
        val workspace = workspaceRepository.save(
            Workspace(
                name = name,
                owner = user,
                description = description,
                workspaceSetting = WorkspaceSetting()
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

    fun checkAccessible(username: String, workspaceId: Long) {
        if (!isAccessible(username, workspaceId)) throw WorkspaceInaccessibleException()
    }

    fun checkCanAccessWorkspace(user: User, workspace: Workspace) {
        if (!isAccessible(user.loginId, workspace.id)) throw WorkspaceInaccessibleException()
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

    fun updateTableCount(workspace: Workspace, tableCount: Int) {
        workspace.tableCount = tableCount
        workspaceRepository.save(workspace)
    }

    fun saveWorkspace(workspace: Workspace): Workspace {
        return workspaceRepository.save(workspace)
    }

    fun deleteWorkspaceImages(workspace: Workspace, deletedImages: List<WorkspaceImage>) {
        workspace.images.removeAll(deletedImages.toSet())
        deletedImages.forEach {
            s3Service.deleteFile(it.url)
        }
    }

    fun saveWorkspaceImages(workspace: Workspace, newImageFiles: List<MultipartFile>): Workspace {
        newImageFiles.forEach {
            val path =
                "$workspacePath/workspace${workspace.id}/workspace/${System.currentTimeMillis()}.jpg"
            val imageUrl = s3Service.uploadFile(it, path)
            workspace.images.add(WorkspaceImage(workspace = workspace, url = imageUrl))
        }
        return workspaceRepository.save(workspace)
    }

    fun getWorkspaceTable(workspace: Workspace, tableNumber: Int): WorkspaceTable {
        return workspaceTableRepository.findByTableNumberAndWorkspace(
            tableNumber,
            workspace
        )
    }

    fun getAllWorkspaceTables(workspace: Workspace): List<WorkspaceTable> {
        return workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace)
    }

    fun updateWorkspaceTables(workspace: Workspace) {
        val currentTableCount = workspaceTableRepository.countAllByWorkspace(workspace)

        if (currentTableCount < workspace.tableCount) {
            val newTables = (currentTableCount until workspace.tableCount).map {
                WorkspaceTable(
                    workspace = workspace,
                    tableNumber = (it + 1).toInt(),
                    tableHash = UUID.randomUUID().toString()
                )
            }

            workspaceTableRepository.saveAll(newTables)
        } else if (currentTableCount > workspace.tableCount) {
            val tablesToRemove =
                workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace)
                    .reversed()
                    .take((currentTableCount - workspace.tableCount).toInt())

            workspaceTableRepository.deleteAll(tablesToRemove)
        }
    }

    fun saveWorkspaceTable(table: WorkspaceTable): WorkspaceTable {
        return workspaceTableRepository.save(table)
    }
}