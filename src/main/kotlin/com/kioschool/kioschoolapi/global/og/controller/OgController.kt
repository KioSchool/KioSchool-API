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
import java.net.URI
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
     * 사장님이 카카오톡/페이스북/인스타 등에 공유하기 위한 링크.
     *
     * - SNS 봇이 fetch하면 og:* 메타태그가 박힌 미니 HTML 응답 → 미리보기 카드 노출
     * - 일반 사용자가 클릭하면 실제 주문 페이지(`/order?workspaceId={id}`)로 302 redirect
     *
     * Amplify rewrite 룰 한 줄(`/share/<*>` → `/og/share/<*>`)만으로 동작하며,
     * UA 기반 분기는 여기서 처리하므로 Amplify의 condition은 필요 없다.
     */
    @GetMapping("/og/share/{workspaceId}")
    fun shareLink(
        @PathVariable workspaceId: Long,
        @RequestHeader(HttpHeaders.USER_AGENT, required = false) userAgent: String?,
    ): ResponseEntity<String> {
        return if (isBot(userAgent)) {
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("${MediaType.TEXT_HTML_VALUE};charset=UTF-8"))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(ogFacade.renderOrderHtml(workspaceId))
        } else {
            ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("$baseUrl/order?workspaceId=$workspaceId"))
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
