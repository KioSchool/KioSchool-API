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

    fun findAllByReferenceDate(referenceDate: LocalDate): List<DailyOrderStatistic>

    fun deleteByWorkspaceId(workspaceId: Long)

    @Query("SELECT COALESCE(SUM(d.totalRevenue), 0) FROM DailyOrderStatistic d")
    fun sumTotalRevenue(): Long

    @Query("SELECT COALESCE(SUM(d.totalOrders), 0) FROM DailyOrderStatistic d")
    fun sumTotalOrders(): Long

    @Query("SELECT COALESCE(SUM(d.totalRevenue), 0) FROM DailyOrderStatistic d WHERE d.referenceDate >= :since")
    fun sumTotalRevenueSince(@Param("since") since: LocalDate): Long

    @Query("SELECT COALESCE(SUM(d.totalOrders), 0) FROM DailyOrderStatistic d WHERE d.referenceDate >= :since")
    fun sumTotalOrdersSince(@Param("since") since: LocalDate): Long

    @Query("SELECT d FROM DailyOrderStatistic d WHERE d.referenceDate >= :since ORDER BY d.referenceDate ASC")
    fun findAllSince(@Param("since") since: LocalDate): List<DailyOrderStatistic>

    @Query("""
        SELECT d.workspace.id, SUM(d.totalRevenue), SUM(d.totalOrders)
        FROM DailyOrderStatistic d
        WHERE d.referenceDate >= :since
        GROUP BY d.workspace.id
        ORDER BY SUM(d.totalRevenue) DESC
    """)
    fun findTopWorkspacesByRevenueSince(@Param("since") since: LocalDate): List<Array<Any>>

    @Query("SELECT COUNT(DISTINCT d.workspace.id) FROM DailyOrderStatistic d WHERE d.referenceDate >= :since AND d.totalOrders > 0")
    fun countActiveWorkspacesSince(@Param("since") since: LocalDate): Long

    @Query("SELECT COALESCE(SUM(d.totalOrders), 0) FROM DailyOrderStatistic d WHERE d.referenceDate >= :since AND d.workspace.id = :workspaceId")
    fun sumTotalOrdersSinceByWorkspace(@Param("since") since: LocalDate, @Param("workspaceId") workspaceId: Long): Long
}
