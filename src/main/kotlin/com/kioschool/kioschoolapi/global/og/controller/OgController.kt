package com.kioschool.kioschoolapi.global.og.controller

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
class OgController(
    private val workspaceRepository: WorkspaceRepository,
    @Value("\${kio.og.fallback-image-url}")
    private val fallbackImageUrl: String,
) {
    @GetMapping("/og/order", produces = [MediaType.TEXT_HTML_VALUE])
    fun ogOrder(@RequestParam(required = false) workspaceId: Long?): ResponseEntity<String> {
        val workspace = workspaceId?.let { workspaceRepository.findById(it).orElse(null) }
        val title = workspace?.let { "${it.name} · 키오스쿨" } ?: "키오스쿨"
        val image = workspace?.ogImageUrl ?: fallbackImageUrl
        val canonical = "https://kio-school.com/order" +
            (workspaceId?.let { "?workspaceId=$it" } ?: "")

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
            .body(renderOgHtml(canonical, title, image))
    }

    private fun renderOgHtml(canonical: String, title: String, image: String): String {
        val titleEsc = htmlEscape(title)
        val canonicalEsc = htmlEscape(canonical)
        val imageEsc = htmlEscape(image)
        return """
            <!doctype html>
            <html lang="ko"><head>
              <meta charset="utf-8">
              <meta property="og:url"         content="$canonicalEsc">
              <meta property="og:title"       content="$titleEsc">
              <meta property="og:description" content="대학 주점 테이블 오더 서비스, 키오스쿨입니다!">
              <meta property="og:type"        content="website">
              <meta property="og:image"       content="$imageEsc">
              <meta property="og:site_name"   content="키오스쿨">
              <title>$titleEsc</title>
            </head><body></body></html>
        """.trimIndent()
    }

    private fun htmlEscape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
