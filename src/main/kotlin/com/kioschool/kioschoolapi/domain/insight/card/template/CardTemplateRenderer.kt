package com.kioschool.kioschoolapi.domain.insight.card.template

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.card.InsightCardSelection
import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import java.awt.image.BufferedImage

interface CardTemplateRenderer {
    val template: CardTemplate
    fun supports(selection: InsightCardSelection): Boolean = selection.template == template
    fun render(selection: InsightCardSelection): BufferedImage
    fun headline(selection: InsightCardSelection): String
    fun payload(selection: InsightCardSelection): CardPayload
}
