package com.kioschool.kioschoolapi.global.og.service

import com.kioschool.kioschoolapi.global.aws.S3Service
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class OgCardGenerator(
    private val s3Service: S3Service,
    @Value("\${cloud.aws.s3.default-path}")
    private val workspacePath: String,
) {
    private val logger = LoggerFactory.getLogger(OgCardGenerator::class.java)

    private val badge: ImmutableImage by lazy {
        val resource = javaClass.getResourceAsStream("/og/og-badge.png")
            ?: error("og-badge.png not found on classpath at /og/og-badge.png")
        resource.use { ImmutableImage.loader().fromStream(it) }
    }

    fun generate(workspaceId: Long, sourcePhotoUrl: String): String {
        val photo = s3Service.downloadFileStream(sourcePhotoUrl).use {
            ImmutableImage.loader().fromStream(it)
        }
        val card = photo.cover(CARD_WIDTH, CARD_HEIGHT)
            .overlay(
                badge,
                CARD_WIDTH - BADGE_MARGIN_X - badge.width,
                CARD_HEIGHT - BADGE_MARGIN_Y - badge.height,
            )
        val bytes = card.bytes(PngWriter())
        val path = pathFor(workspaceId, sourcePhotoUrl)
        val url = s3Service.uploadBytes(bytes, path, "image/png")
        logger.info("OG card generated: workspaceId={}, path={}", workspaceId, path)
        return url
    }

    fun expectedUrl(workspaceId: Long, sourcePhotoUrl: String): String =
        s3Service.urlFor(pathFor(workspaceId, sourcePhotoUrl))

    private fun pathFor(workspaceId: Long, sourcePhotoUrl: String): String {
        val hash = sha1(sourcePhotoUrl).take(8)
        return "$workspacePath/workspace${workspaceId}/og/$hash.png"
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val CARD_WIDTH = 1200
        const val CARD_HEIGHT = 630
        const val BADGE_MARGIN_X = 26
        const val BADGE_MARGIN_Y = 22
    }
}
