package com.kioschool.kioschoolapi.domain.workspace.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.TablePositionUpdateDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceImageSlot
import com.kioschool.kioschoolapi.domain.workspace.entity.*
import com.kioschool.kioschoolapi.domain.workspace.repository.CustomWorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceMemberRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceTableRepository
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.cache.annotation.WorkspaceUpdateEvent
import org.springframework.data.repository.findByIdOrNull
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.dto.FieldErrorDetail
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.security.CustomUserDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class WorkspaceService(
    @Value("\${cloud.aws.s3.default-path}")
    private val workspacePath: String,
    val workspaceRepository: WorkspaceRepository,
    val customWorkspaceRepository: CustomWorkspaceRepository,
    val workspaceTableRepository: WorkspaceTableRepository,
    val workspaceMemberRepository: WorkspaceMemberRepository,
    val userService: UserService,
    val s3Service: S3Service
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllWorkspaces(name: String?, page: Int, size: Int, updatedAfter: LocalDateTime? = null): Page<Workspace> {
        return customWorkspaceRepository.findAllByCondition(
            name,
            PageRequest.of(page, size),
            updatedAfter
        )
    }

    fun checkCanCreateWorkspace(user: User) {
        if (user.account == null) throw CustomException(ErrorCode.NO_PERMISSION_TO_CREATE_WORKSPACE)
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
        if (workspace.invitations.none { it.user == user }) throw CustomException(ErrorCode.NO_PERMISSION_TO_JOIN_WORKSPACE)
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
        return workspaceRepository.findById(workspaceId)
            .orElseThrow { CustomException(ErrorCode.WORKSPACE_NOT_FOUND) }
    }

    fun findWorkspaceOrNull(workspaceId: Long): Workspace? =
        workspaceRepository.findByIdOrNull(workspaceId)

    fun isAccessible(username: String, workspaceId: Long): Boolean {
        val userRole =
            (SecurityContextHolder.getContext().authentication?.principal as? CustomUserDetails)
                ?.takeIf { it.loginId == username }
                ?.role
                ?: userService.getUser(username).role

        if (userRole == UserRole.SUPER_ADMIN) return true

        return workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(workspaceId, username)
    }

    fun checkAccessible(username: String, workspaceId: Long) {
        if (!isAccessible(username, workspaceId)) throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
    }

    fun checkCanAccessWorkspace(user: User, workspace: Workspace) {
        if (user.role == UserRole.SUPER_ADMIN) return
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserLoginId(
                workspace.id,
                user.loginId
            )
        ) {
            throw CustomException(ErrorCode.WORKSPACE_INACCESSIBLE)
        }
    }

    fun checkCanInviteWorkspace(user: User, workspace: Workspace) {
        if (workspace.owner != user) throw CustomException(ErrorCode.NO_PERMISSION_TO_INVITE)
    }

    @Transactional
    @WorkspaceUpdateEvent
    fun inviteUserToWorkspace(workspace: Workspace, user: User): Workspace {
        val workspaceInvitation = WorkspaceInvitation(
            workspace = workspace,
            user = user
        )
        workspace.invitations.add(workspaceInvitation)
        return workspaceRepository.save(workspace)
    }

    @Transactional
    @WorkspaceUpdateEvent
    fun removeUserFromWorkspace(workspace: Workspace, user: User): Workspace {
        workspace.members.removeIf { it.user == user }
        return workspaceRepository.save(workspace)
    }

    @Transactional
    @WorkspaceUpdateEvent
    fun updateTableCount(workspace: Workspace, tableCount: Int): Workspace {
        workspace.tableCount = tableCount
        return workspaceRepository.save(workspace)
    }

    @Transactional
    @WorkspaceUpdateEvent
    fun updateIsOnboarding(workspace: Workspace, isOnboarding: Boolean): Workspace {
        workspace.isOnboarding = isOnboarding
        return workspaceRepository.save(workspace)
    }

    @Transactional
    @WorkspaceUpdateEvent
    fun saveWorkspace(workspace: Workspace): Workspace {
        return workspaceRepository.save(workspace)
    }

    fun deleteWorkspaceImages(workspace: Workspace, deletedImages: List<WorkspaceImage>) {
        workspace.images.removeAll(deletedImages.toSet())
        deletedImages.forEach {
            s3Service.deleteFile(it.url)
        }
    }

    @Transactional
    @WorkspaceUpdateEvent
    fun applyImageSlots(workspace: Workspace, slots: List<WorkspaceImageSlot>): Workspace {
        val imagesById = workspace.images.associateBy { it.id }

        slots.forEachIndexed { index, slot ->
            when (slot) {
                is WorkspaceImageSlot.Existing -> {
                    // 존재 검증은 workspace의 실제 이미지를 알 수 없는 UpdateWorkspaceImageRequestBody.toSlots()에서
                    // 여기로 미뤄졌다. workspace.images는 워크스페이스 범위로 한정된 관계라 다른 워크스페이스의
                    // 이미지가 섞일 일이 없으므로, 존재하지 않거나 오래된 imageId는 여기서 조용히 무시해도 안전하다.
                    val image = imagesById[slot.imageId] ?: return@forEachIndexed
                    slot.focalPoint?.let {
                        image.focalX = it.x
                        image.focalY = it.y
                    }
                }

                is WorkspaceImageSlot.New -> {
                    // 한 요청에 여러 장을 올리면 currentTimeMillis가 같을 수 있어 슬롯 인덱스로 구분한다.
                    val path =
                        "$workspacePath/workspace${workspace.id}/workspace/${System.currentTimeMillis()}-$index.webp"
                    val imageUrl = s3Service.uploadResizedWebpImage(slot.file.inputStream, path)
                    val focalPoint = slot.focalPoint ?: FocalPointDto.CENTER
                    workspace.images.add(
                        WorkspaceImage(
                            workspace = workspace,
                            url = imageUrl,
                            focalX = focalPoint.x,
                            focalY = focalPoint.y,
                        )
                    )
                }
            }
        }

        return workspaceRepository.save(workspace)
    }

    fun getWorkspaceTable(workspace: Workspace, tableNumber: Int): WorkspaceTable {
        if (tableNumber < 1 || tableNumber > workspace.tableCount) {
            throw CustomException(ErrorCode.WORKSPACE_TABLE_NOT_FOUND)
        }

        return workspaceTableRepository.findByTableNumberAndWorkspace(
            tableNumber,
            workspace
        )
    }

    fun getAllWorkspaceTables(workspace: Workspace): List<WorkspaceTable> {
        return workspaceTableRepository
            .findAllByWorkspaceAndTableNumberLessThanEqualOrderByTableNumber(
                workspace,
                workspace.tableCount
            )
    }

    // 관리자 화면용 뷰(getAllWorkspaceTables)와 달리 tableCount 범위 밖 테이블까지 포함한다.
    // 정리(cleanup) 경로는 반드시 이 접근자를 써야 한다 — 슬라이스된 뷰를 쓰면 범위 밖 테이블의
    // orderSession 참조가 끊기지 않아 FK 제약 위반이 난다.
    fun getAllWorkspaceTablesIncludingOutOfRange(workspace: Workspace): List<WorkspaceTable> {
        return workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace)
    }

    @Transactional
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
            // row를 삭제하면 tableHash가 소실되어 인쇄된 QR이 영구 무효화된다.
            // 범위 밖 테이블은 남겨두고 배치 좌표만 비운다.
            val outOfRangeTables =
                workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace)
                    .filter { it.tableNumber > workspace.tableCount }

            val strandedSessions = outOfRangeTables.filter { it.orderSession != null }
            if (strandedSessions.isNotEmpty()) {
                log.warn(
                    "tableCount decrease to {} stranded active sessions on workspace {} tables {}. " +
                        "These tables are no longer visible to admins and their QR codes will 404 until " +
                        "tableCount is raised again or the daily scheduler closes the sessions.",
                    workspace.tableCount,
                    workspace.id,
                    strandedSessions.map { it.tableNumber }
                )
            }

            outOfRangeTables.forEach {
                it.positionX = null
                it.positionY = null
            }
            workspaceTableRepository.saveAll(outOfRangeTables)
        }
    }

    @Transactional
    fun updateTablePosition(
        workspace: Workspace,
        tableId: Long,
        x: Int?,
        y: Int?
    ): WorkspaceTable {
        val table = workspaceTableRepository.findByIdAndWorkspace(tableId, workspace)
            .orElseThrow { CustomException(ErrorCode.WORKSPACE_TABLE_NOT_FOUND) }

        if (x == null || y == null) {
            table.positionX = null
            table.positionY = null
            return workspaceTableRepository.save(table)
        }

        if (x < 0 || y < 0 || x >= MAX_GRID_SIZE || y >= MAX_GRID_SIZE) {
            throw CustomException(ErrorCode.INVALID_TABLE_POSITION)
        }

        val isOccupied = workspaceTableRepository.existsByWorkspaceAndPositionXAndPositionYAndIdNot(
            workspace, x, y, tableId
        )
        if (isOccupied) throw CustomException(ErrorCode.TABLE_POSITION_CONFLICT)

        table.positionX = x
        table.positionY = y
        return workspaceTableRepository.save(table)
    }

    /**
     * 편집 모드의 "저장" 한 번을 그대로 반영한다. 검증을 모두 통과하기 전에는 엔티티를 건드리지
     * 않으므로 부분 적용이 남지 않는다.
     *
     * 충돌 판정은 요청을 다 반영한 **최종 상태** 기준이다. 순서대로 검사하면 자리를 서로 맞바꾸는
     * 재배치(1번을 2번 자리로, 2번을 1번 자리로)가 항상 409로 튕긴다.
     */
    @Transactional
    fun updateTablePositions(workspace: Workspace, updates: List<TablePositionUpdateDto>) {
        val duplicatedTableIds = updates.groupingBy { it.tableId }.eachCount()
            .filterValues { it > 1 }.keys
        if (duplicatedTableIds.isNotEmpty()) {
            throw CustomException(
                ErrorCode.INVALID_INPUT,
                "한 요청에 같은 테이블이 두 번 들어올 수 없습니다: $duplicatedTableIds"
            )
        }

        val tables = workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace)
        val tablesById = tables.associateBy { it.id }

        updates.forEach { update ->
            if (!tablesById.containsKey(update.tableId)) {
                throw CustomException(ErrorCode.WORKSPACE_TABLE_NOT_FOUND)
            }

            val position = update.position ?: return@forEach
            if (position.x < 0 || position.y < 0 ||
                position.x >= MAX_GRID_SIZE || position.y >= MAX_GRID_SIZE
            ) {
                throw CustomException(ErrorCode.INVALID_TABLE_POSITION)
            }
        }

        // 요청에 없는 테이블은 지금 좌표를 그대로 유지한다 -- 그 칸도 여전히 점유 상태다.
        val finalPositions = tables.associate {
            it.id to TablePositionDto.of(it.positionX, it.positionY)
        }.toMutableMap()
        updates.forEach { finalPositions[it.tableId] = it.position }

        val updatedTableIds = updates.map { it.tableId }.toSet()
        val occupants = mutableMapOf<TablePositionDto, Long>()
        finalPositions.forEach { (tableId, position) ->
            if (position == null) return@forEach
            val previousOccupant = occupants.putIfAbsent(position, tableId) ?: return@forEach

            // 이번 요청이 건드리지 않은 두 테이블끼리 이미 겹쳐 있다면(동시 저장 경합의 흔적 등)
            // 그건 이 요청의 잘못이 아니다. 여기서 막으면 관리자가 저장 자체를 못 하게 된다.
            if (tableId !in updatedTableIds && previousOccupant !in updatedTableIds) {
                log.warn(
                    "Pre-existing table position collision on workspace {} at ({}, {}): tables {} and {}",
                    workspace.id, position.x, position.y, previousOccupant, tableId
                )
                return@forEach
            }

            throw conflictOf(position, updates)
        }

        val changed = updates.mapNotNull { update ->
            val table = tablesById.getValue(update.tableId)
            val position = update.position
            if (table.positionX == position?.x && table.positionY == position?.y) return@mapNotNull null

            table.positionX = position?.x
            table.positionY = position?.y
            table
        }
        if (changed.isNotEmpty()) workspaceTableRepository.saveAll(changed)
    }

    // 충돌한 칸을 요청 본문의 위치로 되짚어준다. 프론트가 격자에서 그 칸을 집어낼 수 있게 하기 위함.
    private fun conflictOf(
        position: TablePositionDto,
        updates: List<TablePositionUpdateDto>
    ): CustomException {
        val index = updates.indexOfLast { it.position == position }
        return CustomException(
            ErrorCode.TABLE_POSITION_CONFLICT,
            errors = listOf(
                FieldErrorDetail(
                    field = if (index >= 0) "positions[$index].position" else "positions",
                    value = "(${position.x}, ${position.y})",
                    reason = ErrorCode.TABLE_POSITION_CONFLICT.defaultMessage,
                    index = index.takeIf { it >= 0 }
                )
            )
        )
    }

    @Transactional
    fun resetTablePositions(workspace: Workspace) {
        val tables = workspaceTableRepository.findAllByWorkspaceOrderByTableNumber(workspace)

        val cleared = tables.filter { it.positionX != null && it.positionY != null }
            .map { "${it.tableNumber}:(${it.positionX},${it.positionY})" }
        if (cleared.isNotEmpty()) {
            log.info("Resetting table positions for workspace {}: {}", workspace.id, cleared)
        }

        tables.forEach {
            it.positionX = null
            it.positionY = null
        }
        workspaceTableRepository.saveAll(tables)
    }

    fun saveWorkspaceTable(table: WorkspaceTable): WorkspaceTable {
        return workspaceTableRepository.save(table)
    }

    fun getWorkspaceTableByHash(workspace: Workspace, tableHash: String): WorkspaceTable {
        val table = workspaceTableRepository.findByTableHashAndWorkspace(tableHash, workspace)
            .orElseThrow { CustomException(ErrorCode.WORKSPACE_TABLE_NOT_FOUND) }

        // tableCount 감소로 보존만 된 테이블은 존재하지 않는 것으로 취급한다.
        if (table.tableNumber > workspace.tableCount) {
            throw CustomException(ErrorCode.WORKSPACE_TABLE_NOT_FOUND)
        }

        return table
    }

    fun deleteWorkspace(workspace: Workspace) {
        workspaceRepository.delete(workspace)
    }

    fun deleteAllWorkspaceTables(workspace: Workspace) {
        val tables = getAllWorkspaceTablesIncludingOutOfRange(workspace)
        workspaceTableRepository.deleteAll(tables)
    }

    @Transactional
    @WorkspaceUpdateEvent
    fun changeWorkspaceOwner(workspace: Workspace, newOwner: User): Workspace {
        // 새 소유자가 워크스페이스 멤버가 아니라면 멤버로 추가
        if (workspace.members.none { it.user.id == newOwner.id }) {
            workspace.members.add(WorkspaceMember(workspace = workspace, user = newOwner))
        }
        workspace.owner = newOwner
        return workspaceRepository.save(workspace)
    }

    companion object {
        // 100x100 = 10,000칸이므로 어떤 현실적인 배치에도 제약이 되지 않는다.
        // 목적은 격자 크기 강제가 아니라 터무니없는 좌표로 테이블이 화면 밖에
        // 갇히는 것을 막는 것이다.
        const val MAX_GRID_SIZE = 100
    }
}