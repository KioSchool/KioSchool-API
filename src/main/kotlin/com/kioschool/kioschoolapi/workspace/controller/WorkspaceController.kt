package com.kioschool.kioschoolapi.workspace.controller

import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.workspace.dto.CreateWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.dto.GetWorkspacesResponseBody
import com.kioschool.kioschoolapi.workspace.dto.JoinWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
class WorkspaceController(
    private val workspaceService: WorkspaceService,
) {
    @GetMapping("/workspaces")
    fun getWorkspaces(authentication: Authentication): GetWorkspacesResponseBody {
        val username = (authentication.principal as CustomUserDetails).username
        return GetWorkspacesResponseBody(workspaceService.getWorkspaces(username))
    }

    @PostMapping("/workspace")
    fun createWorkspace(
        authentication: Authentication,
        @RequestBody body: CreateWorkspaceRequestBody
    ): Workspace {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.createWorkspace(username, body.name)
    }

    @PostMapping("/workspace/join")
    fun joinWorkspace(
        authentication: Authentication,
        @RequestBody body: JoinWorkspaceRequestBody
    ): Workspace {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.joinWorkspace(username, body.workspaceId)
    }
}