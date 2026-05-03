package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import jakarta.persistence.*
import jakarta.persistence.JoinColumn

@Entity
@Table(
    name = "order",
    schema = "PUBLIC",
    indexes = [
        Index(name = "idx_order_workspace_created_status", columnList = "workspace_id, created_at, status"),
        Index(name = "idx_order_session_id", columnList = "order_session_id")
    ]
)
class Order(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @JsonIgnore
    val workspace: Workspace,
    val tableNumber: Int,
    val customerName: String,
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    @OrderBy("id")
    val orderProducts: MutableList<OrderProduct> = mutableListOf(),
    var totalPrice: Int = 0,
    var status: OrderStatus = OrderStatus.NOT_PAID,
    val orderNumber: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_session_id")
    val orderSession: OrderSession?
) : BaseEntity()