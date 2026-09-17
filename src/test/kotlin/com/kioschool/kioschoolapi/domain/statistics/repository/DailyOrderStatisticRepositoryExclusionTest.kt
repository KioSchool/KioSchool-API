package com.kioschool.kioschoolapi.domain.statistics.repository

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import org.springframework.data.jpa.repository.Query

class DailyOrderStatisticRepositoryExclusionTest : DescribeSpec({

    describe("슈퍼어드민 대시보드 집계 쿼리") {
        val dashboardQueryMethods = listOf(
            "sumTotalRevenue",
            "sumTotalOrders",
            "sumTotalRevenueSince",
            "sumTotalOrdersSince",
            "findAllSince",
            "findTopWorkspacesByRevenueSince",
            "countActiveWorkspacesSince"
        )

        dashboardQueryMethods.forEach { methodName ->
            it("$methodName 은 축제 달력에서 제외한 통계를 집계에서 뺀다") {
                val method = DailyOrderStatisticRepository::class.java.methods.first { it.name == methodName }
                val query = method.getAnnotation(Query::class.java).value

                query shouldContain "d.excludedFromCalendar = false"
            }
        }
    }
})
