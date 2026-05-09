package com.kioschool.kioschoolapi.global.og.service

import com.kioschool.kioschoolapi.global.aws.S3Service
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStream
import java.security.MessageDigest

@Component
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

    fun generateUrl(workspaceId: Long, sourcePhotoUrl: String): String {
        // 다운로드 단계에서 byte 사이즈 캡. scrimage 디코딩 전에 차단해서 OOM 방어.
        val sourceBytes = s3Service.downloadFileStream(sourcePhotoUrl).use {
            it.readBoundedBytes(MAX_SOURCE_BYTES)
        }
        val photo = ImmutableImage.loader().fromBytes(sourceBytes)
        // 디코딩에 성공한 사진도 거대한 픽셀(예: 6000×4000)일 수 있음.
        // cover 전에 max dimension을 캡해서 합성 단계의 메모리 사용을 묶는다.
        val capped = if (photo.width > MAX_INPUT_DIM || photo.height > MAX_INPUT_DIM) {
            photo.max(MAX_INPUT_DIM, MAX_INPUT_DIM)
        } else photo
        val card = capped.cover(CARD_WIDTH, CARD_HEIGHT)
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

    private fun InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
        // maxBytes + 1 까지 읽어 초과 여부 판별.
        val buffer = this.readNBytes(maxBytes + 1)
        if (buffer.size > maxBytes) {
            throw IllegalStateException(
                "Source photo exceeds size cap ${maxBytes}B (read at least ${buffer.size}B)"
            )
        }
        return buffer
    }

    fun getExpectedUrl(workspaceId: Long, sourcePhotoUrl: String): String =
        s3Service.getPublicUrl(pathFor(workspaceId, sourcePhotoUrl))

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
        // 압축된 JPEG/PNG 기준. 8MB면 디코딩 시 ~수십 MB까지 늘어날 수 있어 보수적.
        const val MAX_SOURCE_BYTES = 8 * 1024 * 1024
        // 디코딩 후 max dimension. 2400×2400 RGBA ≈ 22MB.
        const val MAX_INPUT_DIM = 2400
    }
}
