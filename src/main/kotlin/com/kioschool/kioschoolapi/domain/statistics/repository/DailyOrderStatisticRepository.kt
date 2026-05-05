package com.kioschool.kioschoolapi.domain.statistics.repository

import com.kioschool.kioschoolapi.domain.statistics.entity.DailyOrderStatistic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface DailyOrderStatisticRepository : JpaRepository<DailyOrderStatistic, Long> {
    fun findByWorkspaceIdAndReferenceDate(workspaceId: Long, referenceDate: LocalDate): Optional<DailyOrderStatistic>

    fun deleteByWorkspaceId(workspaceId: Long)

    @Query("SELECT COALESCE(SUM(d.totalRevenue), 0) FROM DailyOrderStatistic d")
    fun sumTotalRevenue(): Long

    @Query("SELECT COALESCE(SUM(d.totalOrders), 0) FROM DailyOrderStatistic d")
    fun sumTotalOrders(): Long

    @Query("SELECT COALESCE(SUM(d.totalRevenue), 0) FROM DailyOrderStatistic d WHERE d.referenceDate >= :since")
    fun sumTotalRevenueSince(@Param("since") since: LocalDate): Long

    @Query("SELECT COALESCE(SUM(d.totalOrders), 0) FROM DailyOrderStatistic d WHERE d.referenceDate >= :since")
    fun sumTotalOrdersSince(@Param("since") since: LocalDate): Long
}
