package com.kioschool.kioschoolapi.domain.insight.card.template

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.card.InsightCardSelection
import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import org.springframework.stereotype.Component
import java.awt.*
import java.awt.image.BufferedImage

@Component
class StoryNumbersTemplate : CardTemplateRenderer {
    override val template = CardTemplate.STORY_NUMBERS

    override fun render(selection: InsightCardSelection): BufferedImage {
        require(selection is InsightCardSelection.StoryNumbers)
        val w = 1200; val h = 1200
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(0x1F, 0x29, 0x37)
        g.fillRect(0, 0, w, h)

        g.color = Color(0xF3, 0xF4, 0xF6)
        g.font = Font("SansSerif", Font.BOLD, 64)
        g.drawString("어제 우리가 만든 숫자", 80, 180)

        val items = listOf(
            "매출" to "₩${"%,d".format(selection.stat.totalRevenue)}",
            "주문" to "${selection.stat.totalOrders}",
            "객단가" to "₩${"%,d".format(selection.stat.averageOrderAmount)}",
            "평균 체류" to "${"%.0f".format(selection.stat.averageStayTimeMinutes)}분"
        )
        items.forEachIndexed { idx, (label, value) ->
            val col = idx % 2; val row = idx / 2
            val x = 100 + col * 560; val y = 380 + row * 320
            g.color = Color(0x9C, 0xA3, 0xAF)
            g.font = Font("SansSerif", Font.PLAIN, 36)
            g.drawString(label, x, y)
            g.color = Color.WHITE
            g.font = Font("SansSerif", Font.BOLD, 96)
            g.drawString(value, x, y + 110)
        }

        g.color = Color(0x9C, 0xA3, 0xAF)
        g.font = Font("SansSerif", Font.PLAIN, 32)
        g.drawString("powered by KioSchool", 80, h - 80)

        g.dispose()
        return img
    }

    override fun headline(selection: InsightCardSelection): String = "어제 우리가 만든 숫자"

    override fun payload(selection: InsightCardSelection): CardPayload {
        require(selection is InsightCardSelection.StoryNumbers)
        return CardPayload(
            totalRevenue = selection.stat.totalRevenue,
            totalOrders = selection.stat.totalOrders,
            averageOrderAmount = selection.stat.averageOrderAmount,
            averageStayMinutes = selection.stat.averageStayTimeMinutes
        )
    }
}
