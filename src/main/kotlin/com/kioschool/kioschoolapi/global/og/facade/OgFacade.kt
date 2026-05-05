package com.kioschool.kioschoolapi.global.og.facade

import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.og.service.OgService
import org.springframework.stereotype.Component

@Component
class OgFacade(
    private val workspaceService: WorkspaceService,
    private val ogService: OgService,
) {
    fun renderOrderHtml(workspaceId: Long?): String {
        val workspace = workspaceId?.let { workspaceService.findWorkspaceOrNull(it) }
        return ogService.renderOrderHtmlFor(workspace, workspaceId)
    }
}
