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

            val cohorts = sut.resolveAll(date)

            // minSize=5, but actual peers count for XS is 1 → falls back to all stats
            // For documentation: when min-size not met, peers = all stats (fallback)
            cohorts[TableCountBucket.XS]?.peers!! shouldContainExactlyInAnyOrder listOf(stat1, stat2)
            cohorts[TableCountBucket.S]?.peers!! shouldContainExactlyInAnyOrder listOf(stat1, stat2)
            cohorts[TableCountBucket.XS]?.bucket shouldBe TableCountBucket.XS
            cohorts[TableCountBucket.S]?.bucket shouldBe TableCountBucket.S
        }
    }
})
