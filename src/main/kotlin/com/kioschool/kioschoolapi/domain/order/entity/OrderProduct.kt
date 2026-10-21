package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
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
    var totalPrice: Int = 0,
    // 주문 당시 세트 1개당 할인 금액. 표시·집계용이고 금액 계산에는 쓰지 않는다. 일반 상품은 null
    val discountPrice: Int? = null
) : BaseEntity() {
    // 세트 주문 항목의 구성품. 일반 상품은 비어 있다.
    @OneToMany(mappedBy = "orderProduct", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder")
    val components: MutableList<OrderProductComponent> = mutableListOf()

    fun isSet(): Boolean = components.isNotEmpty()

    // 세트의 servedCount는 구성품이 모두 나간 세트 수다. 기존 화면·쿼리가 servedCount, isServed를 그대로 읽으므로 저장해 둔다.
    fun recalculateServedCountFromComponents() {
        if (!isSet()) return
        servedCount = components.minOf { it.servedCount / it.quantity }
        isServed = servedCount == quantity
    }

    // 세트 setCount개분을 모두 내준 상태로 맞춘다. 구성품을 따로 더 내준 상태였어도 덮어쓴다.
    fun serveAllComponents(setCount: Int) {
        components.forEach { it.servedCount = it.quantity * setCount }
        servedCount = setCount
        isServed = setCount == quantity
    }
}
