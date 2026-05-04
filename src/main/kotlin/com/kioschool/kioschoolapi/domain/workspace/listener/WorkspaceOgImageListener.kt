package com.kioschool.kioschoolapi.domain.workspace.listener

import com.kioschool.kioschoolapi.domain.workspace.event.WorkspaceUpdatedEvent
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WorkspaceOgImageListener(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
) {
    private val logger = LoggerFactory.getLogger(WorkspaceOgImageListener::class.java)

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: WorkspaceUpdatedEvent) {
        val workspace = workspaceRepository.findById(event.workspaceId).orElse(null) ?: return
        val primaryPhotoUrl = workspace.images.minByOrNull { it.id }?.url

        if (primaryPhotoUrl == null) {
            // No images: clear any stale og card so we fall back to defaults.
            if (workspace.ogImageUrl != null) {
                workspace.ogImageUrl = null
                workspaceRepository.save(workspace)
            }
            return
        }

        // Hash precheck: WorkspaceUpdatedEvent fires for non-photo changes too
        // (name/memo/tableCount/etc.). If the source photo did not change, the
        // expected url stays the same and we can skip the expensive regen.
        val expectedOgUrl = ogCardGenerator.expectedUrl(workspace.id, primaryPhotoUrl)
        if (workspace.ogImageUrl == expectedOgUrl) return

        val newOgUrl = runCatching { ogCardGenerator.generate(workspace.id, primaryPhotoUrl) }
            .getOrElse { ex ->
                // Preserve the previously-good og card. Surfacing via logger.error keeps
                // the user-facing flow intact; Sentry can be wired in later.
                logger.error(
                    "OG card generation failed for workspaceId={}, source={}",
                    workspace.id,
                    primaryPhotoUrl,
                    ex,
                )
                return
            }
        workspace.ogImageUrl = newOgUrl
        workspaceRepository.save(workspace)
    }
}
