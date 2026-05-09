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
     * - 사람 → 실제 주문 페이지로 redirect URI. tableNo/tableHash가 들어오면
     *   query string에 보존해서 친구가 같은 테이블 세션에 합류 가능하도록.
     */
    fun resolveShareLink(
        workspaceId: Long,
        tableNo: Int?,
        tableHash: String?,
        userAgent: String?,
    ): ShareLinkAction {
        return if (isBot(userAgent)) {
            ShareLinkAction.RenderOgHtml(renderOrderHtml(workspaceId))
        } else {
            // 프론트 SPA가 query string에서 `tableNo`를 읽는 컨벤션을 사용하므로
            // 들어온 이름과 redirect URL에 박는 이름 모두 `tableNo`로 맞춘다.
            val target = UriComponentsBuilder.fromUriString("$baseUrl/order")
                .queryParam("workspaceId", workspaceId)
                .apply {
                    tableNo?.let { queryParam("tableNo", it) }
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
        // 봇 전용 토큰만 매칭. webview/in-app browser가 자기 식별자(KAKAOTALK 9.7.0,
        // NAVER, Daum 등)를 UA에 포함시키는 케이스가 많아서 광범위한 토큰은 사용자를
        // 봇으로 오인시킨다. 카카오톡 모바일 in-app browser 사용자가 share link를
        // 클릭했을 때 og HTML을 받아 빈 화면을 보던 사고를 막기 위해 정밀화.
        //
        // 봇 전용 식별자 매핑:
        //   - 카카오톡 preview crawler:    Kakaotalk-Scrap (in-app은 KAKAOTALK/x.x.x (INAPP))
        //   - 카카오스토리 preview crawler: kakaostory-Scrap
        //   - 네이버 검색 봇:              Yeti
        //   - 다음 검색 봇:                Daumoa
        //   - WhatsApp link preview:      WhatsApp/x.x.x (슬래시로 in-app과 구분)
        private val BOT_PATTERN: Pattern = Pattern.compile(
            "(?i)(facebookexternalhit|Kakaotalk-Scrap|kakaostory-Scrap|Slackbot|Twitterbot|" +
                "Discordbot|TelegramBot|LinkedInBot|WhatsApp/|Yeti|Daumoa|Googlebot|bingbot)"
        )
    }
}

sealed class ShareLinkAction {
    data class RenderOgHtml(val body: String) : ShareLinkAction()
    data class RedirectToOrder(val target: URI) : ShareLinkAction()
}
