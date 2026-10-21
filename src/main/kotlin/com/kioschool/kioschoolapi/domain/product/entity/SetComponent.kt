package com.kioschool.kioschoolapi.domain.product.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// 세트에서 실제로 내주는 구성품. 이름과 수량만 있는 글자 정보이고, 품절은 연결 상품(conditionProduct)을 따라간다.
@Entity
@Table(
    name = "set_component",
    indexes = [
        Index(name = "idx_set_component_set_product_id", columnList = "set_product_id"),
        Index(name = "idx_set_component_condition_product_id", columnList = "condition_product_id")
    ]
)
class SetComponent(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_product_id")
    @JsonIgnore
    val setProduct: Product,
    var name: String,
    var quantity: Int,
    var displayOrder: Int,
    // 같은 주점의 일반 상품. 이 상품이 품절이면 세트도 주문할 수 없다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_product_id")
    var conditionProduct: Product? = null
) : BaseEntity()
