package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "order_session")
class OrderSession(
    @ManyToOne
    @JsonIgnore
    val workspace: Workspace,
    var expectedEndAt: LocalDateTime?,
    var endAt: LocalDateTime? = null,
    val tableNumber: Int,
    var usageTime: Int = 0,
    var totalOrderPrice: Long = 0,
    var orderCount: Int = 0,
    var isGhostSession: Boolean = false,
) : BaseEntity()