package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn

@Entity
@Table(
    name = "order_product",
    indexes = [
        Index(name = "idx_order_product_order_id", columnList = "order_id")
    ]
)
class OrderProduct(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
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