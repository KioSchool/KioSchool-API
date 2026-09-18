package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.CohortContext
import com.kioschool.kioschoolapi.domain.insight.service.metric.TableCountBucket
import com.kioschool.kioschoolapi.domain.statistics.dto.PopularProducts
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
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
     * 모든 워크스페이스를 tableCount 버킷으로 그룹핑.
     * 코호트 인원이 minSize 미만인 버킷은 전체 풀을 fallback으로 사용.
     *
     * 배치는 주문 없는 날의 통계 row를 만들지 않으므로, row가 없는 워크스페이스는 0 실적으로 채워
     * 비교 풀(분모·하위 카운트·평균)에 포함시킨다.
     */
    fun resolveAll(referenceDate: LocalDate): Map<TableCountBucket, CohortContext> {
        val recorded = statisticRepository.findAllByReferenceDate(referenceDate)
        val recordedWorkspaceIds = recorded.map { it.workspace.id }.toSet()
        val idle = workspaceRepository.findAll()
            .filter { it.id !in recordedWorkspaceIds }
            .map { idleStatistic(it, referenceDate) }
        val stats = recorded + idle
        val edges = properties.cohort.bucketEdges
        val minSize = properties.cohort.minSize

        val groupedByBucket = stats.groupBy { TableCountBucket.resolve(it.workspace.tableCount, edges) }

        return TableCountBucket.values().associateWith { bucket ->
            val peers = groupedByBucket[bucket].orEmpty()
            val effective = if (peers.size >= minSize) peers else stats // fallback
            CohortContext(bucket = bucket, peers = effective)
        }
    }

    // 영속화하지 않는 메모리 전용 객체
    private fun idleStatistic(workspace: Workspace, referenceDate: LocalDate) = DailyOrderStatistic(
        workspace = workspace,
        referenceDate = referenceDate,
        totalSalesVolume = 0,
        totalRevenue = 0,
        averageOrderAmount = 0,
        totalOrders = 0,
        averageOrdersPerTable = 0.0,
        tableTurnoverRate = 0.0,
        averageStayTimeMinutes = 0.0,
        previousDayComparison = null,
        salesByHour = emptyList(),
        popularProducts = PopularProducts(emptyList(), emptyList(), emptyList())
    )
}
