package com.kioschool.kioschoolapi.workspace.controller

import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class WorkspaceController(
    private val workspaceService: WorkspaceService
) {
    @Operation(summary = "워크스페이스 조회", description = "워크스페이스를 조회합니다.")
    @GetMapping("/workspace")
    fun getWorkspace(
        @RequestParam workspaceId: Long
    ): Workspace {
        return workspaceService.getWorkspace(workspaceId)
    }
}