package com.kioschool.kioschoolapi.domain.workspace.controller

import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceTableDto
import com.kioschool.kioschoolapi.domain.workspace.dto.request.*
import com.kioschool.kioschoolapi.domain.workspace.facade.WorkspaceFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Admin Workspace Controller")
@RestController
@RequestMapping("/admin")
class AdminWorkspaceController(
    private val workspaceFacade: WorkspaceFacade
) {
    @Operation(summary = "워크스페이스 조회", description = "가입한 모든 워크스페이스를 조회합니다.")
    @GetMapping("/workspaces")
    fun getWorkspaces(@AdminUsername username: String): List<WorkspaceDto> {
        return workspaceFacade.getWorkspaces(username)
    }

    @Operation(summary = "워크스페이스 조회", description = "워크스페이스를 조회합니다.")
    @GetMapping("/workspace")
    fun getWorkspace(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long
    ): WorkspaceDto {
        return workspaceFacade.getWorkspace(username, workspaceId)
    }

    @Operation(summary = "워크스페이스 생성", description = "워크스페이스를 생성합니다.")
    @PostMapping("/workspace")
    fun createWorkspace(
        @AdminUsername username: String,
        @RequestBody body: CreateWorkspaceRequestBody
    ): WorkspaceDto {
        return workspaceFacade.createWorkspace(
            username,
            body.name,
            body.description
        )
    }

    @Operation(summary = "워크스페이스 정보 수정", description = "워크스페이스 정보를 수정합니다.")
    @PutMapping("/workspace/info")
    fun updateWorkspace(
        @AdminUsername username: String,
        @RequestBody body: UpdateWorkspaceRequestBody,
    ): WorkspaceDto {
        return workspaceFacade.updateWorkspaceInfo(
            username,
            body.workspaceId,
            body.name,
            body.description,
            body.notice
        )
    }

    @Operation(summary = "워크스페이스 메모 수정", description = "워크스페이스 메모를 수정합니다.")
    @PutMapping("/workspace/memo")
    fun updateWorkspaceMemo(
        @AdminUsername username: String,
        @RequestBody body: UpdateWorkspaceMemoRequestBody,
    ): WorkspaceDto {
        return workspaceFacade.updateWorkspaceMemo(
            username,
            body.workspaceId,
            body.memo
        )
    }

    @Operation(summary = "워크스페이스 이미지 수정", description = "워크스페이스 이미지를 수정합니다.")
    @PutMapping("/workspace/image")
    fun updateWorkspaceImage(
        @AdminUsername username: String,
        @RequestPart body: UpdateWorkspaceImageRequestBody,
        @RequestPart(required = false) imageFiles: List<MultipartFile>?,
    ): WorkspaceDto {
        return workspaceFacade.updateWorkspaceImage(
            username,
            body.workspaceId,
            body.imageIds,
            imageFiles ?: emptyList()
        )
    }

    @Operation(summary = "워크스페이스 초대", description = "워크스페이스에 사용자를 초대합니다.")
    @PostMapping("/workspace/invite")
    fun inviteWorkspace(
        @AdminUsername username: String,
        @RequestBody body: InviteWorkspaceRequestBody
    ): WorkspaceDto {
        return workspaceFacade.inviteWorkspace(
            username,
            body.workspaceId,
            body.userLoginId
        )
    }

    @Operation(summary = "워크스페이스 가입", description = "워크스페이스에 가입합니다.<br>초대를 받은 사용자만 가입할 수 있습니다.")
    @PostMapping("/workspace/join")
    fun joinWorkspace(
        @AdminUsername username: String,
        @RequestBody body: JoinWorkspaceRequestBody
    ): WorkspaceDto {
        return workspaceFacade.joinWorkspace(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 탈퇴", description = "워크스페이스에서 탈퇴합니다.")
    @PostMapping("/workspace/leave")
    fun leaveWorkspace(
        @AdminUsername username: String,
        @RequestBody body: LeaveWorkspaceRequestBody
    ): WorkspaceDto {
        return workspaceFacade.leaveWorkspace(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 테이블 개수 수정", description = "워크스페이스의 테이블 개수를 수정합니다.")
    @PostMapping("/workspace/table-count")
    fun updateTableCount(
        @AdminUsername username: String,
        @Valid @RequestBody body: UpdateTableCountRequestBody
    ): WorkspaceDto {
        return workspaceFacade.updateTableCount(
            username,
            body.workspaceId,
            body.tableCount
        )
    }

    @Operation(summary = "워크스페이스 온보딩 상태 수정", description = "워크스페이스의 온보딩 진행 상태를 수정합니다.")
    @PostMapping("/workspace/onboarding")
    fun updateIsOnboarding(
        @AdminUsername username: String,
        @RequestBody body: UpdateIsOnboardingRequestBody
    ): WorkspaceDto {
        return workspaceFacade.updateIsOnboarding(
            username,
            body.workspaceId,
            body.isOnboarding
        )
    }

    @Operation(
        summary = "워크스페이스 테이블 전체 조회",
        description = "워크스페이스의 테이블을 조회합니다. tableCount 범위 내의 테이블만 반환합니다."
    )
    @GetMapping("/workspace/tables")
    fun getWorkspaceTables(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long
    ): List<WorkspaceTableDto> {
        return workspaceFacade.getAllWorkspaceTables(username, workspaceId)
    }

    @Operation(
        summary = "워크스페이스 테이블 위치 수정",
        description = "테이블의 격자 위치를 수정합니다. position이 null이면 배치를 취소합니다. " +
            "x, y는 0 이상 100 미만이어야 하며 벗어나면 400 INVALID_TABLE_POSITION, " +
            "다른 테이블이 이미 그 칸을 점유하고 있으면 409 TABLE_POSITION_CONFLICT를 반환합니다."
    )
    @PatchMapping("/workspace/table/position")
    fun updateTablePosition(
        @AdminUsername username: String,
        @RequestBody body: UpdateTablePositionRequestBody
    ): WorkspaceTableDto {
        return workspaceFacade.updateTablePosition(
            username,
            body.workspaceId,
            body.tableId,
            body.position
        )
    }

    @Operation(
        summary = "워크스페이스 테이블 위치 일괄 수정",
        description = "편집 모드의 저장 한 번을 반영합니다. 여러 테이블의 격자 위치를 한 요청으로 바꾸며, " +
            "하나라도 실패하면 아무것도 저장되지 않습니다. position이 null인 항목은 배치를 취소합니다. " +
            "충돌 판정은 요청을 모두 반영한 최종 상태 기준이라 서로 자리를 맞바꾸는 재배치도 통과합니다. " +
            "요청에 없는 테이블의 좌표는 그대로 유지되며 그 칸도 여전히 점유 상태로 봅니다. " +
            "같은 테이블이 두 번 들어오면 400 INVALID_INPUT, x, y가 0 이상 100 미만을 벗어나면 " +
            "400 INVALID_TABLE_POSITION, 최종 상태에 겹치는 칸이 있으면 409 TABLE_POSITION_CONFLICT를 " +
            "반환합니다. 409의 errors[0]에는 걸린 칸의 좌표와 요청 내 위치가 담깁니다. " +
            "응답은 GET /workspace/tables와 같은 뷰입니다."
    )
    @PatchMapping("/workspace/table/positions")
    fun updateTablePositions(
        @AdminUsername username: String,
        @Valid @RequestBody body: UpdateTablePositionsRequestBody
    ): List<WorkspaceTableDto> {
        return workspaceFacade.updateTablePositions(username, body.workspaceId, body.positions)
    }

    @Operation(
        summary = "워크스페이스 테이블 위치 초기화",
        description = "워크스페이스의 모든 테이블 위치를 미배치 상태로 되돌립니다." +
            " tableCount 범위 밖 테이블의 좌표도 함께 비우지만, 응답에는 범위 내 테이블만 포함됩니다."
    )
    @PostMapping("/workspace/table/positions/reset")
    fun resetTablePositions(
        @AdminUsername username: String,
        @RequestBody body: ResetTablePositionsRequestBody
    ): List<WorkspaceTableDto> {
        return workspaceFacade.resetTablePositions(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 주문 설정 변경", description = "워크스페이스의 설정 중 주문 관련한 설정을 변경합니다.")
    @PutMapping("/workspace/setting/order")
    fun updateOrderSetting(
        @AdminUsername username: String,
        @RequestBody body: UpdateOrderSettingRequestBody
    ): WorkspaceDto {
        return workspaceFacade.updateOrderSetting(
            username,
            body.workspaceId,
            body.useOrderSessionTimeLimit,
            body.orderSessionTimeLimitMinutes,
        )
    }
}
