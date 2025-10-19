package com.kioschool.kioschoolapi.global.schedule.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "executed_one_time_script")
class ExecutedOneTimeScript(
    @Column(nullable = false, unique = true)
    val scriptName: String,
) : BaseEntity()