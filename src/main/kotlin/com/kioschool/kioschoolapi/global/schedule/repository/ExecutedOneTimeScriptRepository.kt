package com.kioschool.kioschoolapi.global.schedule.repository

import com.kioschool.kioschoolapi.global.schedule.entity.ExecutedOneTimeScript
import org.springframework.data.jpa.repository.JpaRepository

interface ExecutedOneTimeScriptRepository : JpaRepository<ExecutedOneTimeScript, Long> {
    fun existsByScriptName(scriptName: String): Boolean
}