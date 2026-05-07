package com.kioschool.kioschoolapi.global.og.controller

import com.kioschool.kioschoolapi.global.og.facade.OgFacade
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.util.regex.Pattern

@RestController
class OgController(
    private val ogFacade: OgFacade,
    @Value("\${kioschool.base-url}")
    private val baseUrl: String,
) {
    @GetMapping("/og/order", produces = [MediaType.TEXT_HTML_VALUE])
    fun ogOrder(@RequestParam(required = false) workspaceId: Long?): ResponseEntity<String> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
            .body(ogFacade.renderOrderHtml(workspaceId))

    /**
     * SNS 공유용 링크 (사장님 매장 일반 공유 + 손님이 자기 테이블 페이지 친구에게 공유).
     *
     * - SNS 봇이 fetch하면 og:* 메타태그가 박힌 미니 HTML 응답 → 미리보기 카드 노출
     *   (카드는 워크스페이스 사진 기반이라 테이블 정보 무관)
     * - 일반 사용자가 클릭하면 실제 주문 페이지로 302 redirect
     *   (tableNumber/tableHash query param이 있으면 redirect URL에 보존 →
     *   친구가 같은 테이블 세션에 합류 가능)
     *
     * Amplify rewrite 룰 한 줄(`/share/<*>` → `/og/share/<*>`)만으로 동작하며,
     * UA 기반 분기는 여기서 처리하므로 Amplify의 condition은 필요 없다.
     */
    @GetMapping("/og/share/{workspaceId}")
    fun shareLink(
        @PathVariable workspaceId: Long,
        @RequestParam(required = false) tableNumber: Int?,
        @RequestParam(required = false) tableHash: String?,
        @RequestHeader(HttpHeaders.USER_AGENT, required = false) userAgent: String?,
    ): ResponseEntity<String> {
        return if (isBot(userAgent)) {
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("${MediaType.TEXT_HTML_VALUE};charset=UTF-8"))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(ogFacade.renderOrderHtml(workspaceId))
        } else {
            val target = UriComponentsBuilder.fromUriString("$baseUrl/order")
                .queryParam("workspaceId", workspaceId)
                .apply {
                    tableNumber?.let { queryParam("tableNumber", it) }
                    tableHash?.let { queryParam("tableHash", it) }
                }
                .build()
                .toUri()
            ResponseEntity.status(HttpStatus.FOUND)
                .location(target)
                .build()
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
