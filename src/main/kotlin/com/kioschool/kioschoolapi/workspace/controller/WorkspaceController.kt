package com.kioschool.kioschoolapi.workspace.controller

import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.workspace.dto.CreateWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.dto.JoinWorkspaceRequestBody
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin")
class WorkspaceController(
    private val workspaceService: WorkspaceService,
) {
    @GetMapping("/workspaces")
    fun getWorkspaces(authentication: Authentication): MutableList<Workspace> {
        val username = (authentication.principal as CustomUserDetails).username
        return workspaceService.getWorkspaces(username)
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