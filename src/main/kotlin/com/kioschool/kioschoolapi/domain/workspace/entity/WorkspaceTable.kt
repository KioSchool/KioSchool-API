package com.kioschool.kioschoolapi.domain.workspace.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "workspace_table")
class WorkspaceTable(
    @ManyToOne
    @JsonIgnore
    val workspace: Workspace,
    val tableNumber: Int,
    @Column(unique = true)
    val tableHash: String,
    @OneToOne
    var orderSession: OrderSession? = null,
) : BaseEntity()