package com.kioschool.kioschoolapi.domain.insight.card

import com.kioschool.kioschoolapi.domain.insight.card.template.CardTemplateRenderer
import com.kioschool.kioschoolapi.global.aws.S3Service
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.imageio.ImageIO

@Component
class InsightCardGenerator(
    private val templates: List<CardTemplateRenderer>,
    private val s3Service: S3Service,
    @Value("\${cloud.aws.s3.default-path}")
    private val workspacePath: String,
) {
    private val logger = LoggerFactory.getLogger(InsightCardGenerator::class.java)

    fun generateAndUpload(
        selection: InsightCardSelection,
        workspaceId: Long,
        date: LocalDate
    ): String {
        val renderer = templates.firstOrNull { it.supports(selection) }
            ?: throw IllegalStateException("No renderer for ${selection.template}")
        val image = renderer.render(selection)

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val bytes = baos.toByteArray()

        val path = "$workspacePath/workspace${workspaceId}/insight-card/${date}.png"
        val url = s3Service.uploadBytes(bytes, path, "image/png")
        logger.info("Insight card generated: workspaceId={}, path={}", workspaceId, path)
        return url
    }
}
