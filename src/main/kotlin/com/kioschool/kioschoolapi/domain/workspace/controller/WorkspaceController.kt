package com.kioschool.kioschoolapi.domain.workspace.controller

import com.kioschool.kioschoolapi.domain.account.dto.common.AccountDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.CustomerWorkspaceDto
import com.kioschool.kioschoolapi.domain.workspace.facade.WorkspaceFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Workspace Controller")
@RestController
class WorkspaceController(
    private val workspaceFacade: WorkspaceFacade
) {
    @Operation(summary = "워크스페이스 조회", description = "손님 주문 화면에 필요한 워크스페이스 정보를 조회합니다.")
    @GetMapping("/workspace")
    fun getWorkspace(
        @RequestParam workspaceId: Long
    ): CustomerWorkspaceDto {
        // 캐시된 WorkspaceDto는 쓰기 권한 확인에도 쓰이므로 그대로 두고, 응답만 줄인다.
        return CustomerWorkspaceDto.of(workspaceFacade.getWorkspace(workspaceId))
    }

    @Operation(summary = "워크스페이스 관리자 계좌 정보 조회", description = "워크스페이스 관리자의 계좌 정보를 조회합니다.")
    @GetMapping("/workspace/account")
    fun getWorkspaceAccount(
        @RequestParam workspaceId: Long
    ): AccountDto? {
        return workspaceFacade.getWorkspaceAccount(workspaceId)
    }
}