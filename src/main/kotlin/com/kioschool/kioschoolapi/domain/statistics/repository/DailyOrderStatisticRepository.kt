package com.kioschool.kioschoolapi.domain.statistics.repository

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface DailyOrderStatisticRepository : JpaRepository<DailyOrderStatistic, Long> {
    fun findByWorkspaceIdAndReferenceDate(workspaceId: Long, referenceDate: LocalDate): Optional<DailyOrderStatistic>
}
