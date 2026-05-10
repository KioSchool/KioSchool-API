package com.kioschool.kioschoolapi.domain.insight.card.template

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.card.InsightCardSelection
import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.image.BufferedImage

@Component
class MilestoneTemplate : CardTemplateRenderer {
    override val template = CardTemplate.MILESTONE

    override fun render(selection: InsightCardSelection): BufferedImage {
        require(selection is InsightCardSelection.Milestone)
        val w = 1200; val h = 1200
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(0x10, 0xB9, 0x81)
        g.fillRect(0, 0, w, h)

        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.BOLD, 110)
        val headlineText = headline(selection)
        val fm = g.fontMetrics
        g.drawString(headlineText, (w - fm.stringWidth(headlineText)) / 2, h / 2)

        g.font = Font("SansSerif", Font.PLAIN, 36)
        g.color = Color(255, 255, 255, 220)
        val brand = "powered by KioSchool"
        val fm2 = g.fontMetrics
        g.drawString(brand, (w - fm2.stringWidth(brand)) / 2, h - 80)

        g.dispose()
        return img
    }

    override fun headline(selection: InsightCardSelection): String {
        require(selection is InsightCardSelection.Milestone)
        return selection.metric.renderHeadline(selection.result)
    }

    override fun payload(selection: InsightCardSelection): CardPayload {
        require(selection is InsightCardSelection.Milestone)
        return CardPayload(
            milestoneStep = selection.result.milestoneStep,
            absoluteValue = selection.result.absoluteValue
        )
    }
}
