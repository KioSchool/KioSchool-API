package com.kioschool.kioschoolapi.domain.order.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// 주문 당시 세트 구성품의 복사본. 세트를 나중에 고쳐도 이미 들어온 주문은 바뀌지 않는다.
@Entity
@Table(
    name = "order_product_component",
    indexes = [
        Index(name = "idx_order_product_component_order_product_id", columnList = "order_product_id")
    ]
)
class OrderProductComponent(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_product_id")
    @JsonIgnore
    val orderProduct: OrderProduct,
    val name: String,
    // 세트 1개당 수량. 세트를 여러 개 주문해도 이 값은 그대로이고, 주문 전체 수량은 totalQuantity()로 계산한다.
    @Column(name = "quantity")
    val quantityPerSet: Int,
    val displayOrder: Int,
    // 주문 전체 기준으로 지금까지 내준 개수. 0 이상 totalQuantity() 이하
    var servedCount: Int = 0
) : BaseEntity() {
    fun totalQuantity(): Int = quantityPerSet * orderProduct.quantity
}
