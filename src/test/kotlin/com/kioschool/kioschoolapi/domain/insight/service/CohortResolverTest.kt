package com.kioschool.kioschoolapi.domain.insight.service

import com.kioschool.kioschoolapi.domain.insight.property.InsightProperties
import com.kioschool.kioschoolapi.domain.insight.service.metric.TableCountBucket
import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import com.kioschool.kioschoolapi.domain.statistics.repository.DailyOrderStatisticRepository
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class CohortResolverTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val statisticRepository = mockk<DailyOrderStatisticRepository>()
    val properties = InsightProperties().apply {
        cohort = InsightProperties.Cohort().apply {
            bucketEdges = listOf(3, 6, 10)
            minSize = 5
        }
    }
    val sut = CohortResolver(workspaceRepository, statisticRepository, properties)

    fun mockWorkspace(id: Long, tableCount: Int): Workspace {
        val ws = mockk<Workspace>()
        every { ws.id } returns id
        every { ws.tableCount } returns tableCount
        return ws
    }

    fun mockStat(workspace: Workspace): DailyOrderStatistic {
        val stat = mockk<DailyOrderStatistic>()
        every { stat.workspace } returns workspace
        return stat
    }

    describe("resolveAll") {
        it("groups workspaces by tableCount bucket and falls back to all stats when minSize not met") {
            val date = LocalDate.of(2026, 5, 9)
            val ws1 = mockWorkspace(id = 1, tableCount = 2)   // XS
            val ws2 = mockWorkspace(id = 2, tableCount = 5)   // S
            val stat1 = mockStat(workspace = ws1)
            val stat2 = mockStat(workspace = ws2)

            every { statisticRepository.findAllByReferenceDate(date) } returns listOf(stat1, stat2)
            every { workspaceRepository.findAll() } returns listOf(ws1, ws2)

            val cohorts = sut.resolveAll(date)

            // minSize=5, but actual peers count for XS is 1 → falls back to all stats
            // For documentation: when min-size not met, peers = all stats (fallback)
            cohorts[TableCountBucket.XS]?.peers!! shouldContainExactlyInAnyOrder listOf(stat1, stat2)
            cohorts[TableCountBucket.S]?.peers!! shouldContainExactlyInAnyOrder listOf(stat1, stat2)
            cohorts[TableCountBucket.XS]?.bucket shouldBe TableCountBucket.XS
            cohorts[TableCountBucket.S]?.bucket shouldBe TableCountBucket.S
        }

        it("통계 row가 없는 워크스페이스는 0 실적으로 비교 풀에 포함한다") {
            val date = LocalDate.of(2026, 9, 16)
            val active = mockWorkspace(id = 1, tableCount = 2)
            val activeStat = mockStat(workspace = active)
            val idleWorkspaces = (2L..5L).map { mockWorkspace(id = it, tableCount = 2) }

            every { statisticRepository.findAllByReferenceDate(date) } returns listOf(activeStat)
            every { workspaceRepository.findAll() } returns listOf(active) + idleWorkspaces

            val peers = sut.resolveAll(date)[TableCountBucket.XS]!!.peers

            peers.size shouldBe 5
            peers.first() shouldBe activeStat
            peers.drop(1).map { it.workspace } shouldContainExactlyInAnyOrder idleWorkspaces
            peers.drop(1).forEach {
                it.totalOrders shouldBe 0
                it.averageOrderAmount shouldBe 0
                it.salesByHour shouldBe emptyList()
            }
        }
    }
})
