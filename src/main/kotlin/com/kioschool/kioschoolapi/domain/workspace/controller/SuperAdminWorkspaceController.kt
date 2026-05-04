package com.kioschool.kioschoolapi.domain.workspace.controller

import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceAdminDetailDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceDto
import com.kioschool.kioschoolapi.domain.workspace.dto.request.ChangeWorkspaceOwnerRequestBody
import com.kioschool.kioschoolapi.domain.workspace.dto.request.ForceDeleteWorkspaceRequestBody
import com.kioschool.kioschoolapi.domain.workspace.facade.WorkspaceFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Workspace Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminWorkspaceController(
    private val workspaceFacade: WorkspaceFacade,
) {
    @Operation(summary = "워크스페이스 목록 조회", description = "모든 워크스페이스를 조회합니다.")
    @GetMapping("/workspaces")
    fun getWorkspaces(
        @RequestParam(required = false) name: String?,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Page<WorkspaceDto> {
        return workspaceFacade.getAllWorkspaces(name, page, size)
    }

    @Operation(
        summary = "워크스페이스 상세 조회",
        description = "워크스페이스의 상세 정보를 조회합니다. 멤버 수, 상품 수 등 운영 관련 정보를 포함합니다."
    )
    @GetMapping("/workspace")
    fun getWorkspaceDetail(
        @RequestParam workspaceId: Long
    ): WorkspaceAdminDetailDto {
        return workspaceFacade.getWorkspaceAdminDetail(workspaceId)
    }

    @Operation(
        summary = "워크스페이스 강제 삭제",
        description = "워크스페이스와 연결된 모든 주문, 통계 데이터를 포함하여 강제 삭제합니다. 복구 불가하므로 주의하세요."
    )
    @DeleteMapping("/workspace")
    fun forceDeleteWorkspace(
        @RequestBody body: ForceDeleteWorkspaceRequestBody
    ): WorkspaceAdminDetailDto {
        return workspaceFacade.forceDeleteWorkspace(body.workspaceId)
    }

    @Operation(
        summary = "워크스페이스 소유자 변경",
        description = "워크스페이스의 소유자를 다른 유저로 변경합니다. 새 소유자가 멤버가 아니면 자동으로 멤버로 추가됩니다."
    )
    @PutMapping("/workspace/owner")
    fun changeWorkspaceOwner(
        @RequestBody body: ChangeWorkspaceOwnerRequestBody
    ): WorkspaceAdminDetailDto {
        return workspaceFacade.changeWorkspaceOwner(body.workspaceId, body.newOwnerLoginId)
    }
}