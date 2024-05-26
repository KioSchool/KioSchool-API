package com.kioschool.kioschoolapi.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_product")
class OrderProduct(
    @ManyToOne
    @JsonIgnore
    val order: Order,
    val productId: Long,
    val productName: String,
    val productPrice: Int,
    var quantity: Int,
    var isServed: Boolean = false,
    var totalPrice: Int = 0
) : BaseEntity()