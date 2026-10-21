package com.kioschool.kioschoolapi.domain.product.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductType
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn

@Entity
@Table(
    name = "product",
    indexes = [
        Index(name = "idx_product_workspace_category_index", columnList = "workspace_id, product_category_id, index")
    ]
)
class Product(
    var name: String,
    var description: String,
    var price: Int,
    var imageUrl: String? = null,
    var status: ProductStatus = ProductStatus.SELLING,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @JsonIgnore
    val workspace: Workspace,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id")
    var productCategory: ProductCategory? = null,
    var index: Int? = null,
    // 만든 뒤에는 일반 ↔ 세트를 바꿀 수 없다.
    @Enumerated(EnumType.STRING)
    val type: ProductType = ProductType.SINGLE,
    // 세트만 채운다. 세트의 price는 originalPrice - discountPrice를 서버가 계산한 값이다.
    var originalPrice: Int? = null,
    var discountPrice: Int? = null
) : BaseEntity() {
    @OneToMany(mappedBy = "setProduct", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder")
    val components: MutableList<SetComponent> = mutableListOf()

    // 판매 가능 여부. 저장하지 않고 읽을 때마다 계산하며, 세트의 수동 status는 바꾸지 않는다.
    // 연결 상품이 HIDDEN인 것은 품절로 보지 않는다.
    fun effectiveStatus(): ProductStatus {
        if (type != ProductType.SET || status != ProductStatus.SELLING) return status
        val conditionSoldOut = components.any { it.conditionProduct?.status == ProductStatus.SOLD_OUT }
        return if (conditionSoldOut) ProductStatus.SOLD_OUT else ProductStatus.SELLING
    }
}
