package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.og.service.OgCardGenerator
import com.kioschool.kioschoolapi.global.schedule.Runnable
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class V08__BackfillOgCards(
    private val workspaceRepository: WorkspaceRepository,
    private val backfillStep: OgBackfillStep,
    private val environment: Environment,
) : Runnable {
    private val logger = LoggerFactory.getLogger(V08__BackfillOgCards::class.java)

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun run() {
        if (environment.activeProfiles.any { it == "local" || it == "default" } || environment.activeProfiles.isEmpty()) {
            logger.info("Skipping V08__BackfillOgCards script in local environment.")
            return
        }
        logger.info("Starting V08__BackfillOgCards script...")

        val workspaceIds = workspaceRepository.findAll().map { it.id }
        var processed = 0
        var skipped = 0
        var failed = 0

        for (workspaceId in workspaceIds) {
            try {
                when (backfillStep.processOne(workspaceId)) {
                    OgBackfillStep.Result.PROCESSED -> processed++
                    OgBackfillStep.Result.SKIPPED -> skipped++
                }
                Thread.sleep(50L)
            } catch (e: Exception) {
                logger.error("Failed to backfill og card for workspaceId=$workspaceId", e)
                failed++
            }
        }
        logger.info(
            "V08__BackfillOgCards complete: processed={}, skipped={}, failed={}",
            processed,
            skipped,
            failed,
        )
    }
}

/**
 * Per-workspace backfill step. Split out of [V08__BackfillOgCards] so that the
 * `@Transactional` proxy is honored when called from `run()` — same-class
 * invocation would bypass the AOP proxy and leave `ws.images` (a lazy collection)
 * inaccessible.
 */
@Component
class OgBackfillStep(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
) {
    enum class Result { PROCESSED, SKIPPED }

    @Transactional
    fun processOne(workspaceId: Long): Result {
        val ws = workspaceRepository.findById(workspaceId).orElse(null)
            ?: return Result.SKIPPED
        if (ws.ogImageUrl != null) return Result.SKIPPED
        val primaryPhotoUrl = ws.images.minByOrNull { it.id }?.url
            ?: return Result.SKIPPED
        val newUrl = ogCardGenerator.generate(ws.id, primaryPhotoUrl)
        ws.ogImageUrl = newUrl
        workspaceRepository.save(ws)
        return Result.PROCESSED
    }
}
