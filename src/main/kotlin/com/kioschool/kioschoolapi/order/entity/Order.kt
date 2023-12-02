package com.kioschool.kioschoolapi.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.common.enums.OrderStatus
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import jakarta.persistence.*

@Entity
@Table(name = "order", schema = "PUBLIC")
class Order(
    @ManyToOne
    @JsonIgnore
    val workspace: Workspace,
    val tableNumber: Int,
    val phoneNumber: String,
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val orderProducts: MutableList<OrderProduct> = mutableListOf(),
    var totalPrice: Int = 0,
    var status: OrderStatus = OrderStatus.NOT_PAID,
) : BaseEntity()