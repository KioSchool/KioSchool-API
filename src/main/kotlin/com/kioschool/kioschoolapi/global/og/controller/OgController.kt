package com.kioschool.kioschoolapi.global.og.controller

import com.kioschool.kioschoolapi.global.og.facade.OgFacade
import com.kioschool.kioschoolapi.global.og.facade.ShareLinkAction
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
import java.time.Duration

@RestController
class OgController(
    private val ogFacade: OgFacade,
) {
    @GetMapping("/og/order", produces = [MediaType.TEXT_HTML_VALUE])
    fun ogOrder(@RequestParam(required = false) workspaceId: Long?): ResponseEntity<String> =
        ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
            .body(ogFacade.renderOrderHtml(workspaceId))

    /**
     * SNS 공유 링크 — 봇/사람 분기는 facade에서 처리하고, controller는
     * facade가 반환한 [ShareLinkAction]을 HTTP 응답으로 래핑만 한다.
     */
    @GetMapping("/og/share/{workspaceId}")
    fun shareLink(
        @PathVariable workspaceId: Long,
        @RequestParam(required = false) tableNo: Int?,
        @RequestParam(required = false) tableHash: String?,
        @RequestHeader(HttpHeaders.USER_AGENT, required = false) userAgent: String?,
    ): ResponseEntity<String> =
        when (val action = ogFacade.resolveShareLink(workspaceId, tableNo, tableHash, userAgent)) {
            is ShareLinkAction.RenderOgHtml ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("${MediaType.TEXT_HTML_VALUE};charset=UTF-8"))
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                    .body(action.body)
            is ShareLinkAction.RedirectToOrder ->
                ResponseEntity.status(HttpStatus.FOUND)
                    .location(action.target)
                    .build()
        }
}
