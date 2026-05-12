package com.kioschool.kioschoolapi.domain.insight.service.metric

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic

data class CohortContext(
    val bucket: TableCountBucket,
    val peers: List<DailyOrderStatistic>
)
