package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudience
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceFilter
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceRow
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRecipientRepository
import com.kioschool.kioschoolapi.domain.insight.repository.DailyInsightCardRepository
import com.kioschool.kioschoolapi.domain.order.dto.common.WorkspaceOrderStats
import com.kioschool.kioschoolapi.domain.order.repository.OrderProductRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class MailAudienceService(
    private val orderRepository: OrderRepository,
    private val orderProductRepository: OrderProductRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val dailyInsightCardRepository: DailyInsightCardRepository,
    private val mailCampaignRecipientRepository: MailCampaignRecipientRepository,
    private val emailService: EmailService,
    @Value("\${mail.campaign.daily-limit}")
    private val dailyLimit: Int,
) {
    private class LastSent(val subject: String, val sentAt: LocalDateTime, val replied: Boolean)

    private class Audience(
        val targets: List<MailAudienceRow>,
        val noEmailCount: Int,
        val unsubscribedCount: Int,
        val withdrawnCount: Int,
    )

    @Transactional(readOnly = true)
    fun list(from: LocalDate, to: LocalDate): MailAudience {
        val audience = audienceOf(workspaceRepository.findAllWithOwner(), from, to)
        return MailAudience(audience.targets, audience.noEmailCount, audience.unsubscribedCount, audience.withdrawnCount, dailyLimit)
    }

    @Transactional(readOnly = true)
    fun resolve(filter: MailAudienceFilter): List<MailAudienceRow> =
        audienceOf(workspaceRepository.findAllWithOwnerByIdIn(filter.workspaceIds), filter.from, filter.to).targets

    private fun audienceOf(candidates: List<Workspace>, from: LocalDate, to: LocalDate): Audience {
        val start = from.atStartOfDay()
        val end = to.plusDays(1).atStartOfDay()
        val orderStats = orderRepository.findWorkspaceOrderStats(start, end).associateBy { it.workspaceId }
        val resolver = emailService.getSchoolResolver()
        val workspaces = candidates.sortedBy { it.id }

        val withdrawn = workspaces.filter { it.owner.withdrawnAt != null }
        val active = workspaces - withdrawn.toSet()
        val noEmail = active.filter { it.owner.email.isNullOrBlank() }
        val reachable = active - noEmail.toSet()
        val unsubscribed = reachable.filter { it.owner.mailUnsubscribedAt != null }
        val targets = reachable - unsubscribed.toSet()

        val targetIds = targets.map { it.id }
        val topMenus = topMenusOf(targetIds, start, end)
        val insightCounts = insightCardCountsOf(targetIds, from, to)
        val lastSent = lastSentOf(targetIds)

        return Audience(
            targets = targets.map { workspace ->
                val sent = lastSent[workspace.id]
                toRow(workspace, resolver.schoolOf(workspace.owner.email), orderStats[workspace.id]).copy(
                    topMenu = topMenus[workspace.id],
                    insightCardCount = insightCounts[workspace.id] ?: 0,
                    lastSentAt = sent?.sentAt,
                    lastSentSubject = sent?.subject,
                    lastReplied = sent?.replied ?: false,
                )
            },
            noEmailCount = noEmail.size,
            unsubscribedCount = unsubscribed.size,
            withdrawnCount = withdrawn.size,
        )
    }

    // 0원 상품은 직원 호출 같은 요청으로 쓰는 곳이 많아 메뉴에서 뺀다
    private fun topMenusOf(workspaceIds: List<Long>, start: LocalDateTime, end: LocalDateTime): Map<Long, String> {
        if (workspaceIds.isEmpty()) return emptyMap()
        return orderProductRepository.findMenuSales(workspaceIds, start, end)
            .groupBy { it.workspaceId }
            .mapValues { (_, sales) -> sales.maxWith(compareBy({ it.quantity }, { it.productName })).productName }
    }

    private fun insightCardCountsOf(workspaceIds: List<Long>, from: LocalDate, to: LocalDate): Map<Long, Long> {
        if (workspaceIds.isEmpty()) return emptyMap()
        return dailyInsightCardRepository.countByWorkspaceIds(workspaceIds, from, to)
            .associate { (workspaceId, count) -> (workspaceId as Number).toLong() to (count as Number).toLong() }
    }

    private fun lastSentOf(workspaceIds: List<Long>): Map<Long, LastSent> {
        if (workspaceIds.isEmpty()) return emptyMap()
        return mailCampaignRecipientRepository.findSentByWorkspaceIds(workspaceIds)
            .map { row -> (row[0] as Number).toLong() to LastSent(row[1] as String, row[2] as LocalDateTime, row[3] == true) }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, sent) -> sent.maxBy { it.sentAt } }
    }

    private fun toRow(workspace: Workspace, schoolName: String, stats: WorkspaceOrderStats?) = MailAudienceRow(
        workspaceId = workspace.id,
        workspaceName = workspace.name,
        schoolName = schoolName,
        ownerName = workspace.owner.name,
        ownerEmail = workspace.owner.email.orEmpty(),
        tableCount = workspace.tableCount,
        orderCount = stats?.orderCount ?: 0,
        revenue = stats?.revenue ?: 0,
        operatingDays = stats?.operatingDays ?: 0,
        firstOperatingDate = stats?.firstOrderAt?.toLocalDate(),
        lastOperatingDate = stats?.lastOrderAt?.toLocalDate(),
    )
}
