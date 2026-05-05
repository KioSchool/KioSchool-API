package com.kioschool.kioschoolapi.domain.workspace.facade

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountDto
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderSessionRepository
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceAdminDetailDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceTableDto
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Component
class WorkspaceFacade(
    val userService: UserService,
    val discordService: DiscordService,
    val workspaceService: WorkspaceService,
    val orderRepository: OrderRepository,
    val orderSessionRepository: OrderSessionRepository,
    val dailyOrderStatisticRepository: DailyOrderStatisticRepository
) {
    fun getAllWorkspaces(name: String?, page: Int, size: Int) =
        workspaceService.getAllWorkspaces(name, page, size).map { WorkspaceDto.of(it) }

    @Cacheable(cacheNames = [CacheNames.WORKSPACES], key = "#workspaceId")
    fun getWorkspace(workspaceId: Long): WorkspaceDto {
        return WorkspaceDto.of(workspaceService.getWorkspace(workspaceId))
    }

    fun getWorkspace(username: String, workspaceId: Long): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)

        return WorkspaceDto.of(workspace)
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

    fun updateIsOnboarding(username: String, workspaceId: Long, isOnboarding: Boolean): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspaceService.updateIsOnboarding(workspace, isOnboarding)

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

    fun updateWorkspaceMemo(username: String, workspaceId: Long, memo: String): WorkspaceDto {
        val user = userService.getUser(username)
        val workspace = workspaceService.getWorkspace(workspaceId)

        workspaceService.checkCanAccessWorkspace(user, workspace)
        workspace.memo = memo

        return WorkspaceDto.of(workspaceService.saveWorkspace(workspace))
    }

    fun getWorkspaceAdminDetail(workspaceId: Long): WorkspaceAdminDetailDto {
        val workspace = workspaceService.getWorkspace(workspaceId)
        return WorkspaceAdminDetailDto.of(workspace)
    }

    @Transactional
    fun forceDeleteWorkspace(workspaceId: Long): WorkspaceAdminDetailDto {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val detail = WorkspaceAdminDetailDto.of(workspace)

        // 1. DailyOrderStatistic 삭제 (Workspace FK)
        dailyOrderStatisticRepository.deleteByWorkspaceId(workspaceId)

        // 2. Order 삭제 - cascade로 OrderProduct도 함께 삭제됨 (Workspace + OrderSession FK)
        val orders = orderRepository.findAllByWorkspaceId(workspaceId)
        orderRepository.deleteAll(orders)

        // 3. WorkspaceTable의 orderSession 참조를 null로 해제 (OrderSession FK)
        val tables = workspaceService.getAllWorkspaceTables(workspace)
        tables.filter { it.orderSession != null }.forEach {
            it.orderSession = null
            workspaceService.saveWorkspaceTable(it)
        }

        // 4. OrderSession 삭제 (Workspace FK)
        val sessions = orderSessionRepository.findAllByWorkspaceId(workspaceId)
        orderSessionRepository.deleteAll(sessions)

        // 5. WorkspaceTable 삭제 (Workspace FK)
        workspaceService.deleteAllWorkspaceTables(workspace)

        // 6. Workspace 삭제 - cascade로 members, images, setting, products, productCategories, invitations 처리
        workspaceService.deleteWorkspace(workspace)
        return detail
    }

    fun changeWorkspaceOwner(workspaceId: Long, newOwnerLoginId: String): WorkspaceAdminDetailDto {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val newOwner = userService.getUser(newOwnerLoginId)
        return WorkspaceAdminDetailDto.of(workspaceService.changeWorkspaceOwner(workspace, newOwner))
    }
}