package com.kioschool.kioschoolapi.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.product.entity.Product
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_product")
class OrderProduct(
    @ManyToOne
    @JsonIgnore
    val order: Order,
    @ManyToOne
    val product: Product,
    var quantity: Int,
    var isServed: Boolean = false,
    var totalPrice: Int = 0
) : BaseEntity()