package com.kioschool.kioschoolapi.global.schedule

import com.kioschool.kioschoolapi.global.schedule.entity.ExecutedOneTimeScript
import com.kioschool.kioschoolapi.global.schedule.repository.ExecutedOneTimeScriptRepository
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

fun interface Runnable {
    fun run()
}

@Profile("batch")
@Component
class OneTimeScheduler(
    private val oneTimeScripts: List<Runnable>,
    private val executedOneTimeScriptRepository: ExecutedOneTimeScriptRepository
) {
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    fun executeOnce() {
        val sortedScripts = oneTimeScripts.sortedBy { it::class.java.simpleName }
        sortedScripts.forEach { script ->
            val className = script::class.java.simpleName
            // Strip V01__, V02__, etc., to keep backward compatibility with scripts already in the database
            val scriptName = className.replace(Regex("^V\\d+__"), "")
            if (!executedOneTimeScriptRepository.existsByScriptName(scriptName)) {
                script.run()
                executedOneTimeScriptRepository.save(
                    ExecutedOneTimeScript(scriptName = scriptName)
                )
            }
        }
    }
}