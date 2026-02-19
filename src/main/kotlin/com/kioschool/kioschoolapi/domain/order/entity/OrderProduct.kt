package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_product")
class OrderProduct(
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    val order: Order,
    val productId: Long,
    val productName: String,
    val productPrice: Int,
    var quantity: Int,
    var servedCount: Int = 0,
    var isServed: Boolean = false,
    var totalPrice: Int = 0
) : BaseEntity()