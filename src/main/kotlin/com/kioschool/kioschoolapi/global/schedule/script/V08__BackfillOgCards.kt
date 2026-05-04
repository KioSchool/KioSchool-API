package com.kioschool.kioschoolapi.global.schedule.script

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
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

    enum class Result { PROCESSED, SKIPPED }

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
                    Result.PROCESSED -> processed++
                    Result.SKIPPED -> skipped++
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

@Component
class OgBackfillStep(
    private val workspaceRepository: WorkspaceRepository,
    private val ogCardGenerator: OgCardGenerator,
) {
    @Transactional
    fun processOne(workspaceId: Long): V08__BackfillOgCards.Result {
        val ws = workspaceRepository.findById(workspaceId).orElse(null)
            ?: return V08__BackfillOgCards.Result.SKIPPED
        if (ws.ogImageUrl != null) return V08__BackfillOgCards.Result.SKIPPED
        val primaryPhotoUrl = ws.images.minByOrNull { it.id }?.url
            ?: return V08__BackfillOgCards.Result.SKIPPED
        val newUrl = ogCardGenerator.generate(ws.id, primaryPhotoUrl)
        ws.ogImageUrl = newUrl
        workspaceRepository.save(ws)
        return V08__BackfillOgCards.Result.PROCESSED
    }
}
