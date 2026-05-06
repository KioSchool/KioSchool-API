package com.kioschool.kioschoolapi.global.schedule

import com.kioschool.kioschoolapi.global.schedule.entity.ExecutedOneTimeScript
import com.kioschool.kioschoolapi.global.schedule.repository.ExecutedOneTimeScriptRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

fun interface Runnable {
    fun run()
}

@Component
class OneTimeScheduler(
    private val oneTimeScripts: List<Runnable>,
    private val executedOneTimeScriptRepository: ExecutedOneTimeScriptRepository,
    private val transactionTemplate: TransactionTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    fun executeOnce() {
        val sortedScripts = oneTimeScripts.sortedBy { it::class.java.simpleName }
        sortedScripts.forEach { script ->
            val className = script::class.java.simpleName
            // Strip V01__, V02__, etc., to keep backward compatibility with scripts already in the database
            val scriptName = className.replace(Regex("^V\\d+__"), "")

            if (executedOneTimeScriptRepository.existsByScriptName(scriptName)) return@forEach

            try {
                transactionTemplate.execute {
                    script.run()
                    executedOneTimeScriptRepository.save(
                        ExecutedOneTimeScript(scriptName = scriptName)
                    )
                }
                log.info("OneTimeScript executed successfully: $scriptName")
            } catch (e: Exception) {
                log.error("OneTimeScript failed, will retry on next startup: $scriptName", e)
            }
        }
    }
}