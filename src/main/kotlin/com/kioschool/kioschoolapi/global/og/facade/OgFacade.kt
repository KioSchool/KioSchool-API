package com.kioschool.kioschoolapi.global.og.facade

import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.og.service.OgService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.regex.Pattern

@Component
class OgFacade(
    private val workspaceService: WorkspaceService,
    private val ogService: OgService,
    @Value("\${kioschool.base-url}")
    private val baseUrl: String,
) {
    fun renderOrderHtml(workspaceId: Long?): String {
        val workspace = workspaceId?.let { workspaceService.findWorkspaceOrNull(it) }
        return ogService.renderOrderHtmlFor(workspace, workspaceId)
    }

    /**
     * `/share/{workspaceId}` 진입을 봇/사람으로 분기.
     *
     * - 봇 → og 메타태그 미니 HTML (워크스페이스 단위 카드, 테이블 정보 무관)
     * - 사람 → 실제 주문 페이지로 redirect URI. tableNumber/tableHash가 들어오면
     *   query string에 보존해서 친구가 같은 테이블 세션에 합류 가능하도록.
     */
    fun resolveShareLink(
        workspaceId: Long,
        tableNumber: Int?,
        tableHash: String?,
        userAgent: String?,
    ): ShareLinkAction {
        return if (isBot(userAgent)) {
            ShareLinkAction.RenderOgHtml(renderOrderHtml(workspaceId))
        } else {
            val target = UriComponentsBuilder.fromUriString("$baseUrl/order")
                .queryParam("workspaceId", workspaceId)
                .apply {
                    tableNumber?.let { queryParam("tableNumber", it) }
                    tableHash?.let { queryParam("tableHash", it) }
                }
                .build()
                .toUri()
            ShareLinkAction.RedirectToOrder(target)
        }
    }

    private fun isBot(userAgent: String?): Boolean {
        if (userAgent.isNullOrBlank()) return false
        return BOT_PATTERN.matcher(userAgent).find()
    }

    companion object {
        private val BOT_PATTERN: Pattern = Pattern.compile(
            "(?i)(facebookexternalhit|KAKAOTALK|kakaostory|Slackbot|Twitterbot|" +
                "Discordbot|TelegramBot|LinkedInBot|WhatsApp|naver|Yeti|Daum|Googlebot|bingbot)"
        )
    }
}

sealed class ShareLinkAction {
    data class RenderOgHtml(val body: String) : ShareLinkAction()
    data class RedirectToOrder(val target: URI) : ShareLinkAction()
}
