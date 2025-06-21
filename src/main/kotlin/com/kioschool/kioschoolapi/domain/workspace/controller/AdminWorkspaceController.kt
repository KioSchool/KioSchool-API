package com.kioschool.kioschoolapi.domain.workspace.controller

import com.kioschool.kioschoolapi.domain.workspace.dto.*
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.facade.WorkspaceFacade
import com.kioschool.kioschoolapi.global.common.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
    fun getWorkspaces(@AdminUsername username: String): List<Workspace> {
        return workspaceFacade.getWorkspaces(username)
    }

    @Operation(summary = "워크스페이스 조회", description = "워크스페이스를 조회합니다.")
    @GetMapping("/workspace")
    fun getWorkspace(
        @AdminUsername username: String,
        @RequestParam workspaceId: Long
    ): Workspace {
        return workspaceFacade.getWorkspace(workspaceId)
    }

    @Operation(summary = "워크스페이스 생성", description = "워크스페이스를 생성합니다.")
    @PostMapping("/workspace")
    fun createWorkspace(
        @AdminUsername username: String,
        @RequestBody body: CreateWorkspaceRequestBody
    ): Workspace {
        return workspaceFacade.createWorkspace(username, body.name, body.description)
    }

    @Operation(summary = "워크스페이스 정보 수정", description = "워크스페이스 정보를 수정합니다.")
    @PutMapping("/workspace/info")
    fun updateWorkspace(
        @AdminUsername username: String,
        @RequestBody body: UpdateWorkspaceRequestBody,
    ): Workspace {
        return workspaceFacade.updateWorkspaceInfo(
            username,
            body.workspaceId,
            body.name,
            body.description,
            body.notice
        )
    }

    @Operation(summary = "워크스페이스 이미지 수정", description = "워크스페이스 이미지를 수정합니다.")
    @PutMapping("/workspace/image")
    fun updateWorkspaceImage(
        @AdminUsername username: String,
        @RequestPart body: UpdateWorkspaceImageRequestBody,
        @RequestPart(required = false) imageFiles: List<MultipartFile>?,
    ): Workspace {
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
    ): Workspace {
        return workspaceFacade.inviteWorkspace(username, body.workspaceId, body.userLoginId)
    }

    @Operation(summary = "워크스페이스 가입", description = "워크스페이스에 가입합니다.<br>초대를 받은 사용자만 가입할 수 있습니다.")
    @PostMapping("/workspace/join")
    fun joinWorkspace(
        @AdminUsername username: String,
        @RequestBody body: JoinWorkspaceRequestBody
    ): Workspace {
        return workspaceFacade.joinWorkspace(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 탈퇴", description = "워크스페이스에서 탈퇴합니다.")
    @PostMapping("/workspace/leave")
    fun leaveWorkspace(
        @AdminUsername username: String,
        @RequestBody body: LeaveWorkspaceRequestBody
    ): Workspace {
        return workspaceFacade.leaveWorkspace(username, body.workspaceId)
    }

    @Operation(summary = "워크스페이스 테이블 개수 수정", description = "워크스페이스의 테이블 개수를 수정합니다.")
    @PostMapping("/workspace/table-count")
    fun updateTableCount(
        @AdminUsername username: String,
        @RequestBody body: UpdateTableCountRequestBody
    ): Workspace {
        return workspaceFacade.updateTableCount(username, body.workspaceId, body.tableCount)
    }
}
