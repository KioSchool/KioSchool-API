package com.kioschool.kioschoolapi.global.schedule

import com.kioschool.kioschoolapi.global.schedule.entity.ExecutedOneTimeScript
import com.kioschool.kioschoolapi.global.schedule.repository.ExecutedOneTimeScriptRepository
import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

fun interface Runnable {
    fun run()
}

@Component
class OneTimeScheduler(
    private val oneTimeScripts: List<Runnable>,
    private val executedOneTimeScriptRepository: ExecutedOneTimeScriptRepository
) {
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    fun executeOnce() {
        oneTimeScripts.forEach { script ->
            val scriptName = script::class.java.simpleName
            if (!executedOneTimeScriptRepository.existsByScriptName(scriptName)) {
                script.run()
                executedOneTimeScriptRepository.save(
                    ExecutedOneTimeScript(scriptName = scriptName)
                )
            }
        }
    }
}