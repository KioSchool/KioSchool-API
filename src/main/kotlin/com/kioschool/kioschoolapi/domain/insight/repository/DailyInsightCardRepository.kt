package com.kioschool.kioschoolapi.domain.insight.repository

import com.kioschool.kioschoolapi.domain.insight.entity.DailyInsightCard
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface DailyInsightCardRepository : JpaRepository<DailyInsightCard, Long> {
    fun findByWorkspaceIdAndReferenceDate(workspaceId: Long, referenceDate: LocalDate): Optional<DailyInsightCard>
    fun findTopByWorkspaceIdOrderByReferenceDateDesc(workspaceId: Long): Optional<DailyInsightCard>
    fun findAllByReferenceDate(referenceDate: LocalDate): List<DailyInsightCard>
    fun deleteByReferenceDate(referenceDate: LocalDate): Long
}
