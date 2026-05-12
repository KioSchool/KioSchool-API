package com.kioschool.kioschoolapi.domain.insight.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "kioschool.insight")
class InsightProperties {
    var thresholdPercentile: Double = 80.0
    var cohort: Cohort = Cohort()
    var milestone: Milestone = Milestone()

    class Cohort {
        var bucketEdges: List<Int> = listOf(3, 6, 10)
        var minSize: Int = 5
    }

    class Milestone {
        var revenueSteps: List<Long> = listOf(1_000_000, 3_000_000, 5_000_000, 10_000_000)
        var tableSteps: List<Int> = listOf(100, 200, 500)
        var orderSteps: List<Int> = listOf(100, 300, 500)
    }
}
