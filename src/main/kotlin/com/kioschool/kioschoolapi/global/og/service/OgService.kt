package com.kioschool.kioschoolapi.global.og.service

import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils

@Service
class OgService(
    private val workspaceService: WorkspaceService,
    private val ogCardGenerator: OgCardGenerator,
    @Value("\${kio-school.og.fallback-image-url}")
    private val fallbackImageUrl: String,
) {
    fun renderOrderHtml(workspaceId: Long?): String {
        val workspace = workspaceId?.let { workspaceService.findWorkspaceOrNull(it) }
        val title = workspace?.let { "${it.name} · 키오스쿨" } ?: "키오스쿨"
        val image = workspace?.ogImageUrl ?: fallbackImageUrl
        val canonical = "https://kio-school.com/order" +
            (workspaceId?.let { "?workspaceId=$it" } ?: "")
        return renderOgHtml(canonical, title, image)
    }

    fun regenerateOgCard(workspaceId: Long, sourcePhotoUrl: String): String =
        ogCardGenerator.generate(workspaceId, sourcePhotoUrl)

    fun predictedOgUrl(workspaceId: Long, sourcePhotoUrl: String): String =
        ogCardGenerator.predictedUrl(workspaceId, sourcePhotoUrl)

    private fun renderOgHtml(canonical: String, title: String, image: String): String {
        val titleEsc = HtmlUtils.htmlEscape(title, "UTF-8")
        val canonicalEsc = HtmlUtils.htmlEscape(canonical, "UTF-8")
        val imageEsc = HtmlUtils.htmlEscape(image, "UTF-8")
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
}
