package com.kioschool.kioschoolapi.domain.insight.repository

import com.kioschool.kioschoolapi.domain.insight.entity.DailyInsightCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.Optional

interface DailyInsightCardRepository : JpaRepository<DailyInsightCard, Long> {
    fun findByWorkspaceIdAndReferenceDate(workspaceId: Long, referenceDate: LocalDate): Optional<DailyInsightCard>
    fun findAllByWorkspaceIdAndReferenceDateGreaterThanEqualOrderByReferenceDateDesc(
        workspaceId: Long,
        referenceDate: LocalDate
    ): List<DailyInsightCard>
    fun findAllByReferenceDate(referenceDate: LocalDate): List<DailyInsightCard>
    fun deleteByReferenceDate(referenceDate: LocalDate): Long

    fun deleteByWorkspaceId(workspaceId: Long)

    // 행: [workspaceId, 카드 수]
    @Query("""
        SELECT d.workspace.id, COUNT(d)
        FROM DailyInsightCard d
        WHERE d.workspace.id IN :workspaceIds AND d.referenceDate >= :from AND d.referenceDate <= :to
        GROUP BY d.workspace.id
    """)
    fun countByWorkspaceIds(
        @Param("workspaceIds") workspaceIds: Collection<Long>,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
    ): List<Array<Any>>
}
