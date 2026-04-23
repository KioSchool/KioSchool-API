package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import jakarta.persistence.*

@Entity
@Table(name = "order", schema = "PUBLIC")
class Order(
    @ManyToOne(fetch = FetchType.LAZY)
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
    val orderSession: OrderSession?
) : BaseEntity()