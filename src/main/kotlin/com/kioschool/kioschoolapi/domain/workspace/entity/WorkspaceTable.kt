package com.kioschool.kioschoolapi.domain.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "workspace_table")
class WorkspaceTable(
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    val workspace: Workspace,
    val tableNumber: Int,
    @Column(unique = true)
    val tableHash: String,
    @OneToOne(fetch = FetchType.LAZY)
    var orderSession: OrderSession? = null,
) : BaseEntity()