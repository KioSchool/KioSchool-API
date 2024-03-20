package com.kioschool.kioschoolapi.workspace.controller

import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.workspace.dto.CreateWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.dto.InviteWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.dto.JoinWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.dto.LeaveWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin Workspace Controller")
@RestController
@RequestMapping("/admin")
class AdminWorkspaceController(
    private val workspaceService: WorkspaceService,
) {

    @Operation(summary = "워크스페이스 조회", description = "가입한 모든 워크스페이스를 조회합니다.")
    @GetMapping("/workspaces")
    fun getWorkspaces(authentication: Authentication): List<Workspace> {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.getWorkspaces(username)
    }

    @Operation(summary = "워크스페이스 생성", description = "워크스페이스를 생성합니다.")
    @PostMapping("/workspace")
    fun createWorkspace(
        authentication: Authentication,
        @RequestBody body: CreateWorkspaceRequestBody
    ): Workspace {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.createWorkspace(username, body.name, body.description)
    }

    @Operation(summary = "워크스페이스 초대", description = "워크스페이스에 사용자를 초대합니다.")
    @PostMapping("/workspace/invite")
    fun inviteWorkspace(
        authentication: Authentication,
        @RequestBody body: InviteWorkspaceRequestBody
    ): Workspace {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.inviteWorkspace(username, body.workspaceId, body.userLoginId)
    }

    @Operation(summary = "워크스페이스 가입", description = "워크스페이스에 가입합니다.<br>초대를 받은 사용자만 가입할 수 있습니다.")
    @PostMapping("/workspace/join")
    fun joinWorkspace(
        authentication: Authentication,
        @RequestBody body: JoinWorkspaceRequestBody
    ): Workspace {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.joinWorkspace(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 탈퇴", description = "워크스페이스에서 탈퇴합니다.")
    @PostMapping("/workspace/leave")
    fun leaveWorkspace(
        authentication: Authentication,
        @RequestBody body: LeaveWorkspaceRequestBody
    ): Workspace {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.leaveWorkspace(username, body.workspaceId)
    }
}