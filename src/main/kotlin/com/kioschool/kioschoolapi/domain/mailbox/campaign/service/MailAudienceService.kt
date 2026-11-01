package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceFilter
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudiencePreview
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceRow
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MailAudienceService(
    private val orderRepository: OrderRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val emailService: EmailService,
    @Value("\${mail.campaign.daily-limit}")
    private val dailyLimit: Int,
) {
    private class Audience(
        val targets: List<MailAudienceRow> = emptyList(),
        val noEmailCount: Int = 0,
        val unsubscribedCount: Int = 0,
        val withdrawnCount: Int = 0,
    )

    @Transactional(readOnly = true)
    fun resolve(filter: MailAudienceFilter): List<MailAudienceRow> = audienceOf(filter).targets

    @Transactional(readOnly = true)
    fun preview(filter: MailAudienceFilter, page: Int, size: Int): MailAudiencePreview {
        val audience = audienceOf(filter)
        val pageable = PageRequest.of(page, size)
        val slice = audience.targets.drop(pageable.offset.toInt()).take(size)
        return MailAudiencePreview(
            targetCount = audience.targets.size,
            noEmailCount = audience.noEmailCount,
            unsubscribedCount = audience.unsubscribedCount,
            withdrawnCount = audience.withdrawnCount,
            dailyLimit = dailyLimit,
            content = PageImpl(slice, pageable, audience.targets.size.toLong()),
        )
    }

    private fun audienceOf(filter: MailAudienceFilter): Audience {
        val workspaceIds = orderRepository.findWorkspaceIdsWithOrders(
            filter.from.atStartOfDay(),
            filter.to.plusDays(1).atStartOfDay(),
            filter.minOrders,
        )
        if (workspaceIds.isEmpty()) return Audience()

        val resolver = emailService.getSchoolResolver()
        val schoolDomains = filter.schoolName?.takeIf { it.isNotBlank() }?.let { resolver.domainsOf(it) }
        val workspaces = workspaceRepository.findAllById(workspaceIds)
            .filter { it.id !in filter.excludedWorkspaceIds }
            .filter { schoolDomains == null || it.owner.email?.substringAfter("@") in schoolDomains }
            .sortedBy { it.id }

        val withdrawn = workspaces.filter { it.owner.withdrawnAt != null }
        val active = workspaces - withdrawn.toSet()
        val noEmail = active.filter { it.owner.email.isNullOrBlank() }
        val reachable = active - noEmail.toSet()
        val unsubscribed = reachable.filter { it.owner.mailUnsubscribedAt != null }
        val targets = reachable - unsubscribed.toSet()

        return Audience(
            targets = targets.map { toRow(it, resolver.schoolOf(it.owner.email)) },
            noEmailCount = noEmail.size,
            unsubscribedCount = unsubscribed.size,
            withdrawnCount = withdrawn.size,
        )
    }

    private fun toRow(workspace: Workspace, schoolName: String) = MailAudienceRow(
        workspaceId = workspace.id,
        workspaceName = workspace.name,
        schoolName = schoolName,
        ownerName = workspace.owner.name,
        ownerEmail = workspace.owner.email.orEmpty(),
    )
}
