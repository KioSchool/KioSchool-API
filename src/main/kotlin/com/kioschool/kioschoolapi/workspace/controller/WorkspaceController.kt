package com.kioschool.kioschoolapi.workspace.controller

import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.facade.WorkspaceFacade
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
    @Operation(summary = "워크스페이스 조회", description = "워크스페이스를 조회합니다.")
    @GetMapping("/workspace")
    fun getWorkspace(
        @RequestParam workspaceId: Long
    ): Workspace {
        return workspaceFacade.getWorkspace(workspaceId)
    }

    @Operation(summary = "워크스페이스 관리자 계좌 정보 조회", description = "워크스페이스 관리자의 계좌 정보를 조회합니다.")
    @GetMapping("/workspace/account")
    fun getWorkspaceAccount(
        @RequestParam workspaceId: Long
    ): String {
        return workspaceFacade.getWorkspaceAccount(workspaceId)
    }
}