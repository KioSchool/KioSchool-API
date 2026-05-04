package com.kioschool.kioschoolapi.global.og.facade

import com.kioschool.kioschoolapi.global.og.service.OgService
import org.springframework.stereotype.Component

@Component
class OgFacade(
    private val ogService: OgService,
) {
    fun renderOrderHtml(workspaceId: Long?): String =
        ogService.renderOrderHtml(workspaceId)
}
