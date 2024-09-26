package com.kioschool.kioschoolapi.workspace.controller

import com.kioschool.kioschoolapi.common.annotation.Username
import com.kioschool.kioschoolapi.workspace.dto.*
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.facade.WorkspaceFacade
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Admin Workspace Controller")
@RestController
@RequestMapping("/admin")
class AdminWorkspaceController(
    private val workspaceFacade: WorkspaceFacade
) {
    @Operation(summary = "워크스페이스 조회", description = "가입한 모든 워크스페이스를 조회합니다.")
    @GetMapping("/workspaces")
    fun getWorkspaces(@Username username: String): List<Workspace> {
        return workspaceFacade.getWorkspaces(username)
    }

    @Operation(summary = "워크스페이스 조회", description = "워크스페이스를 조회합니다.")
    @GetMapping("/workspace")
    fun getWorkspace(
        @Username username: String,
        @RequestParam workspaceId: Long
    ): Workspace {
        return workspaceFacade.getWorkspace(workspaceId)
    }

    @Operation(summary = "워크스페이스 생성", description = "워크스페이스를 생성합니다.")
    @PostMapping("/workspace")
    fun createWorkspace(
        @Username username: String,
        @RequestBody body: CreateWorkspaceRequestBody
    ): Workspace {
        return workspaceFacade.createWorkspace(username, body.name, body.description)
    }

    @Operation(summary = "워크스페이스 초대", description = "워크스페이스에 사용자를 초대합니다.")
    @PostMapping("/workspace/invite")
    fun inviteWorkspace(
        @Username username: String,
        @RequestBody body: InviteWorkspaceRequestBody
    ): Workspace {
        return workspaceFacade.inviteWorkspace(username, body.workspaceId, body.userLoginId)
    }

    @Operation(summary = "워크스페이스 가입", description = "워크스페이스에 가입합니다.<br>초대를 받은 사용자만 가입할 수 있습니다.")
    @PostMapping("/workspace/join")
    fun joinWorkspace(
        @Username username: String,
        @RequestBody body: JoinWorkspaceRequestBody
    ): Workspace {
        return workspaceFacade.joinWorkspace(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 탈퇴", description = "워크스페이스에서 탈퇴합니다.")
    @PostMapping("/workspace/leave")
    fun leaveWorkspace(
        @Username username: String,
        @RequestBody body: LeaveWorkspaceRequestBody
    ): Workspace {
        return workspaceFacade.leaveWorkspace(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 테이블 개수 수정", description = "워크스페이스의 테이블 개수를 수정합니다.")
    @PostMapping("/workspace/table")
    fun updateTableCount(
        @Username username: String,
        @RequestBody body: UpdateTableCountRequestBody
    ): Workspace {
        return workspaceFacade.updateTableCount(username, body.workspaceId, body.tableCount)
    }
}
