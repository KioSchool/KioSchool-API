package com.kioschool.kioschoolapi.domain.insight.card.template

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.card.InsightCardSelection
import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.image.BufferedImage

@Component
class SingleTrophyTemplate : CardTemplateRenderer {
    override val template = CardTemplate.SINGLE_TROPHY

    override fun render(selection: InsightCardSelection): BufferedImage {
        require(selection is InsightCardSelection.SingleTrophy)
        val w = 1200; val h = 1200
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // gradient bg
        val gp = GradientPaint(0f, 0f, Color(0xFB, 0xBF, 0x24), 0f, h.toFloat(), Color(0xF5, 0x9E, 0x0B))
        g.paint = gp
        g.fillRect(0, 0, w, h)

        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.BOLD, 220)
        val medal = "🥇"
        val fm1 = g.fontMetrics
        g.drawString(medal, (w - fm1.stringWidth(medal)) / 2, h / 2 - 100)

        g.font = Font("SansSerif", Font.BOLD, 90)
        val headlineText = headline(selection)
        val fm2 = g.fontMetrics
        g.drawString(headlineText, (w - fm2.stringWidth(headlineText)) / 2, h / 2 + 200)

        g.font = Font("SansSerif", Font.PLAIN, 36)
        g.color = Color(255, 255, 255, 200)
        val brand = "powered by KioSchool"
        val fm3 = g.fontMetrics
        g.drawString(brand, (w - fm3.stringWidth(brand)) / 2, h - 80)

        g.dispose()
        return img
    }

    override fun headline(selection: InsightCardSelection): String {
        require(selection is InsightCardSelection.SingleTrophy)
        return selection.metric.renderHeadline(selection.result)
    }

    override fun payload(selection: InsightCardSelection): CardPayload {
        require(selection is InsightCardSelection.SingleTrophy)
        return CardPayload(
            cohortAverageRatio = selection.result.cohortAverageRatio,
            absoluteValue = selection.result.absoluteValue
        )
    }
}
