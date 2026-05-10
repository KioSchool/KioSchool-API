package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.TableCountBucket
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CohortResolver(
    private val workspaceRepository: WorkspaceRepository,
    private val statisticRepository: DailyOrderStatisticRepository,
    private val properties: InsightProperties
) {
    /**
     * referenceDate에 통계가 있는 모든 워크스페이스를 tableCount 버킷으로 그룹핑.
     * 코호트 인원이 minSize 미만인 버킷은 전체 풀을 fallback으로 사용.
     */
    fun resolveAll(referenceDate: LocalDate): Map<TableCountBucket, CohortContext> {
        val stats = statisticRepository.findAllByReferenceDate(referenceDate)
        val edges = properties.cohort.bucketEdges
        val minSize = properties.cohort.minSize

        val groupedByBucket = stats.groupBy { TableCountBucket.resolve(it.workspace.tableCount, edges) }

        return TableCountBucket.values().associateWith { bucket ->
            val peers = groupedByBucket[bucket].orEmpty()
            val effective = if (peers.size >= minSize) peers else stats // fallback
            CohortContext(bucket = bucket, peers = effective)
        }
    }
}
