package com.kioschool.kioschoolapi.domain.insight.facade

import com.kioschool.kioschoolapi.domain.insight.dto.InsightCardResponse
import com.kioschool.kioschoolapi.domain.insight.service.InsightCardService
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import org.springframework.stereotype.Component

@Component
class InsightCardFacade(
    private val workspaceService: WorkspaceService,
    private val insightCardService: InsightCardService
) {
    fun get(username: String, workspaceId: Long): InsightCardResponse? {
        workspaceService.checkAccessible(username, workspaceId)
        return insightCardService.findLatest(workspaceId)
    }
}
