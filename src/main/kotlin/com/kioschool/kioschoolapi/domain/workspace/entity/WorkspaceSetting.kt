package com.kioschool.kioschoolapi.domain.workspace.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "workspace_setting")
class WorkspaceSetting(
    var useOrderSessionTimeLimit: Boolean = true,
    var orderSessionTimeLimitMinutes: Long = 90,
) : BaseEntity()