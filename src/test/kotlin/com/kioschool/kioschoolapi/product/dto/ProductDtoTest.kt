package com.kioschool.kioschoolapi.product.dto

import com.kioschool.kioschoolapi.domain.product.dto.common.AdminProductDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ConditionProductDto
import com.kioschool.kioschoolapi.domain.product.dto.common.LinkedSetDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import com.kioschool.kioschoolapi.factory.SampleEntity.newProduct
import com.kioschool.kioschoolapi.factory.SampleEntity.newSetProduct
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus.HIDDEN
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus.SELLING
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus.SOLD_OUT
import com.kioschool.kioschoolapi.global.common.enums.ProductType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ProductDtoTest : DescribeSpec({
    describe("손님용 ProductDto") {
        it("status는 계산된 판매 상태, discountPrice는 세트 할인 금액") {
            val dto = ProductDto.of(newSetProduct(20, listOf(newProduct(10), newProduct(11, status = SOLD_OUT))))
            dto.status shouldBe SOLD_OUT
            dto.price shouldBe 25000
            dto.discountPrice shouldBe 3000
        }

        it("일반 상품은 discountPrice가 null") {
            ProductDto.of(newProduct(10)).discountPrice shouldBe null
        }
    }

    describe("관리자용 AdminProductDto") {
        it("수동 status와 effectiveStatus를 따로 준다") {
            val set = newSetProduct(20, listOf(newProduct(10), newProduct(11, name = "막국수", status = SOLD_OUT)))

            val dto = AdminProductDto.of(set, emptyList())

            dto.type shouldBe ProductType.SET
            dto.status shouldBe SELLING
            dto.effectiveStatus shouldBe SOLD_OUT
            dto.price shouldBe 25000
            dto.originalPrice shouldBe 28000
            dto.discountPrice shouldBe 3000
            dto.components.map { it.displayOrder } shouldBe listOf(0, 1)
            dto.components[1].conditionProduct shouldBe ConditionProductDto(11, "막국수", SOLD_OUT)
            dto.linkedSets shouldBe emptyList()
        }

        it("연결 상품이 없는 구성품은 conditionProduct가 null") {
            AdminProductDto.of(newSetProduct(20, listOf(null)), emptyList())
                .components.single().conditionProduct shouldBe null
        }

        it("일반 상품은 구성품이 비고 연결 세트를 싣는다") {
            val single = newProduct(10)
            val set = newSetProduct(20, listOf(single), status = HIDDEN)

            val dto = AdminProductDto.of(single, listOf(set))

            dto.type shouldBe ProductType.SINGLE
            dto.components shouldBe emptyList()
            dto.linkedSets shouldBe listOf(LinkedSetDto(20, "set20", HIDDEN))
        }
    }
})
