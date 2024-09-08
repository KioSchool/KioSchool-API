package com.kioschool.kioschoolapi.workspace.controller

import com.kioschool.kioschoolapi.common.annotation.Username
import com.kioschool.kioschoolapi.user.exception.NoPermissionException
import com.kioschool.kioschoolapi.user.service.UserService
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Super Admin Workspace Controller")
@RestController
@RequestMapping("/super-admin")
class SuperAdminWorkspaceController(
    private val userService: UserService,
    private val workspaceService: WorkspaceService,
) {
    @Operation(summary = "워크스페이스 조회", description = "모든 워크스페이스를 조회합니다.")
    @GetMapping("/workspaces")
    fun getWorkspaces(
        @Username username: String,
        @RequestParam(required = false) name: String?,
        @RequestParam page: Int,
        @RequestParam size: Int
    ): Page<Workspace> {
        if (!userService.isSuperAdminUser(username)) throw NoPermissionException()

        return workspaceService.getAllWorkspaces(name, page, size)
    }
}