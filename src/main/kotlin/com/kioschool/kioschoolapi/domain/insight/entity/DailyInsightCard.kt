package com.kioschool.kioschoolapi.domain.insight.entity

import com.kioschool.kioschoolapi.domain.insight.card.CardTemplate
import com.kioschool.kioschoolapi.domain.insight.entity.converter.CardPayloadConverter
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "daily_insight_card",
    indexes = [Index(name = "idx_dic_workspace_date", columnList = "workspace_id, reference_date", unique = true)]
)
class DailyInsightCard(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    val workspace: Workspace,

    val referenceDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    val template: CardTemplate,

    @Column(length = 64)
    val bestMetricKey: String?,

    val bestMetricPercentile: Double?,

    @Column(length = 255)
    val headline: String,

    @Column(length = 512)
    val imageUrl: String,

    @Convert(converter = CardPayloadConverter::class)
    @Column(columnDefinition = "TEXT")
    val payload: CardPayload
) : BaseEntity()
