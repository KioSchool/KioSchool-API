package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.Index
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import java.time.LocalDateTime

@Entity
@Table(
    name = "order_session",
    indexes = [
        Index(name = "idx_order_session_workspace_created", columnList = "workspace_id, created_at")
    ]
)
class OrderSession(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @JsonIgnore
    val workspace: Workspace,
    var expectedEndAt: LocalDateTime?,
    var endAt: LocalDateTime? = null,
    val tableNumber: Int,
    var usageTime: Int = 0,
    var totalOrderPrice: Long = 0,
    var orderCount: Int = 0,
    @Enumerated(EnumType.STRING)
    var ghostType: GhostType = GhostType.NONE,
    var customerName: String? = null
) : BaseEntity()