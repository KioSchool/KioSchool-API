package com.kioschool.kioschoolapi.domain.donation.facade

import com.kioschool.kioschoolapi.domain.donation.dto.common.CustomerDonationClickItemDto
import com.kioschool.kioschoolapi.domain.donation.dto.common.CustomerDonationClickStatsDto
import com.kioschool.kioschoolapi.domain.donation.entity.CustomerDonationClick
import com.kioschool.kioschoolapi.domain.donation.repository.CustomerDonationClickRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Component
class SuperAdminDonationFacade(
    private val customerDonationClickRepository: CustomerDonationClickRepository,
    private val orderRepository: OrderRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    fun getCustomerClickStats(startDate: LocalDate, endDate: LocalDate): CustomerDonationClickStatsDto {
        if (startDate.isAfter(endDate) || ChronoUnit.DAYS.between(startDate, endDate) >= MAX_RANGE_DAYS) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        // 날짜는 키오스쿨 영업일(09:00 ~ 익일 08:59) 기준. CustomerDonationService의 "오늘"과 동일 규칙.
        val start = startDate.atTime(BUSINESS_DAY_START)
        val end = endDate.plusDays(1).atTime(BUSINESS_DAY_START)

        val clicks = customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)
        val ordersInRange = orderRepository.countByStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            OrderStatus.CANCELLED, start, end
        )

        val totalClicks = clicks.size.toLong()
        val uniqueOrders = uniqueOrdersOf(clicks)
        val clickedAmounts = clicks.mapNotNull { it.amount }
        val clickedAmountSum = clickedAmounts.sumOf { it.toLong() }
        val depositedClicks = clicks.filter { it.isDeposited }
        val depositedOrders = uniqueOrdersOf(depositedClicks)

        val summary = CustomerDonationClickStatsDto.Summary(
            totalClicks = totalClicks,
            uniqueOrders = uniqueOrders,
            clickedAmountSum = clickedAmountSum,
            averageAmount = if (clickedAmounts.isNotEmpty()) clickedAmountSum / clickedAmounts.size else 0L,
            ordersInRange = ordersInRange,
            clickRatePerOrder = if (ordersInRange > 0) uniqueOrders.toDouble() / ordersInRange else 0.0,
            depositedClicks = depositedClicks.size.toLong(),
            depositedOrders = depositedOrders,
            depositAmountSum = depositAmountSumOf(depositedClicks),
            depositRatePerOrder = if (uniqueOrders > 0) depositedOrders.toDouble() / uniqueOrders else 0.0
        )

        return CustomerDonationClickStatsDto(
            startDate = startDate.format(DATE_FORMATTER),
            endDate = endDate.format(DATE_FORMATTER),
            summary = summary,
            daily = buildDaily(clicks, startDate, endDate),
            byAmount = bucketsOf(clicks) { it.amount?.toString() },
            byMethod = bucketsOf(clicks) { it.method },
            byNoteIndex = bucketsOf(clicks) { it.noteIndex?.toString() },
            topWorkspaces = buildTopWorkspaces(clicks)
        )
    }

    private fun buildDaily(
        clicks: List<CustomerDonationClick>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CustomerDonationClickStatsDto.DailyPoint> {
        val clicksByDate = clicks.groupBy { businessDateOf(it.createdAt) }
        return generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .map { date ->
                val dayClicks = clicksByDate[date].orEmpty()
                CustomerDonationClickStatsDto.DailyPoint(
                    date = date.format(DATE_FORMATTER),
                    clicks = dayClicks.size.toLong(),
                    uniqueOrders = uniqueOrdersOf(dayClicks),
                    amountSum = dayClicks.sumOf { (it.amount ?: 0).toLong() },
                    depositAmountSum = depositAmountSumOf(dayClicks)
                )
            }
            .toList()
    }

    private fun bucketsOf(
        clicks: List<CustomerDonationClick>,
        keySelector: (CustomerDonationClick) -> String?
    ): List<CustomerDonationClickStatsDto.Bucket> {
        val total = clicks.size
        return clicks.groupingBy(keySelector).eachCount()
            .map { (key, count) ->
                CustomerDonationClickStatsDto.Bucket(
                    key = key,
                    clicks = count.toLong(),
                    ratio = if (total > 0) count.toDouble() / total else 0.0
                )
            }
            .sortedByDescending { it.clicks }
    }

    private fun buildTopWorkspaces(clicks: List<CustomerDonationClick>): List<CustomerDonationClickStatsDto.WorkspaceItem> {
        val top = clicks.filter { it.workspaceId != null }
            .groupBy { it.workspaceId!! }
            .entries
            .sortedByDescending { it.value.size }
            .take(TOP_WORKSPACE_LIMIT)
        val workspaceNames = workspaceRepository.findAllById(top.map { it.key }).associate { it.id to it.name }

        return top.map { (workspaceId, workspaceClicks) ->
            CustomerDonationClickStatsDto.WorkspaceItem(
                workspaceId = workspaceId,
                workspaceName = workspaceNames[workspaceId],
                clicks = workspaceClicks.size.toLong(),
                uniqueOrders = uniqueOrdersOf(workspaceClicks),
                amountSum = workspaceClicks.sumOf { (it.amount ?: 0).toLong() },
                depositAmountSum = depositAmountSumOf(workspaceClicks)
            )
        }
    }

    fun getCustomerClickItems(date: LocalDate): List<CustomerDonationClickItemDto> {
        val clicks = customerDonationClickRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            date.atTime(BUSINESS_DAY_START),
            date.plusDays(1).atTime(BUSINESS_DAY_START)
        ).sortedBy { it.createdAt }
        val workspaceNames = workspaceNamesOf(clicks.mapNotNull { it.workspaceId }.distinct())

        return clicks.map { CustomerDonationClickItemDto.of(it, workspaceNames[it.workspaceId]) }
    }

    fun confirmDeposit(clickId: Long, amount: Int, memo: String?): CustomerDonationClickItemDto {
        val normalizedMemo = memo?.trim()?.takeIf { it.isNotEmpty() }
        if (amount < MIN_DEPOSIT_AMOUNT || (normalizedMemo?.length ?: 0) > MAX_DEPOSIT_MEMO_LENGTH) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }

        val click = findClick(clickId)
        click.confirmDeposit(amount, normalizedMemo, LocalDateTime.now())
        return toItem(customerDonationClickRepository.save(click))
    }

    fun cancelDeposit(clickId: Long): CustomerDonationClickItemDto {
        val click = findClick(clickId)
        click.cancelDeposit()
        return toItem(customerDonationClickRepository.save(click))
    }

    private fun findClick(clickId: Long): CustomerDonationClick =
        customerDonationClickRepository.findByIdOrNull(clickId)
            ?: throw CustomException(ErrorCode.DONATION_CLICK_NOT_FOUND)

    private fun toItem(click: CustomerDonationClick): CustomerDonationClickItemDto {
        val workspaceNames = workspaceNamesOf(listOfNotNull(click.workspaceId))
        return CustomerDonationClickItemDto.of(click, workspaceNames[click.workspaceId])
    }

    private fun workspaceNamesOf(workspaceIds: List<Long>): Map<Long, String> {
        if (workspaceIds.isEmpty()) return emptyMap()
        return workspaceRepository.findAllById(workspaceIds).associate { it.id to it.name }
    }

    private fun depositAmountSumOf(clicks: List<CustomerDonationClick>): Long =
        clicks.sumOf { (it.depositAmount ?: 0).toLong() }

    private fun uniqueOrdersOf(clicks: List<CustomerDonationClick>): Long =
        clicks.mapNotNull { it.orderId }.distinct().size.toLong()

    private fun businessDateOf(createdAt: LocalDateTime?): LocalDate? =
        createdAt?.minusHours(BUSINESS_DAY_START.hour.toLong())?.toLocalDate()

    companion object {
        private val BUSINESS_DAY_START: LocalTime = LocalTime.of(9, 0)
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
        private const val MAX_RANGE_DAYS = 366L
        private const val TOP_WORKSPACE_LIMIT = 10
        private const val MIN_DEPOSIT_AMOUNT = 1
        private const val MAX_DEPOSIT_MEMO_LENGTH = 100
    }
}
