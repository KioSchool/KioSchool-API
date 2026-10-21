package com.kioschool.kioschoolapi.product.entity

import com.kioschool.kioschoolapi.factory.SampleEntity.newProduct
import com.kioschool.kioschoolapi.factory.SampleEntity.newSetProduct
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus.HIDDEN
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus.SELLING
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus.SOLD_OUT
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ProductEffectiveStatusTest : DescribeSpec({
    describe("Product.effectiveStatus") {
        it("일반 상품은 status 그대로") {
            newProduct(1, status = SOLD_OUT).effectiveStatus() shouldBe SOLD_OUT
            newProduct(2, status = HIDDEN).effectiveStatus() shouldBe HIDDEN
            newProduct(3).effectiveStatus() shouldBe SELLING
        }

        it("세트가 HIDDEN이면 연결 상품이 품절이어도 HIDDEN") {
            newSetProduct(20, listOf(newProduct(10, status = SOLD_OUT)), status = HIDDEN)
                .effectiveStatus() shouldBe HIDDEN
        }

        it("세트가 SOLD_OUT이면 연결 상품이 모두 판매 중이어도 SOLD_OUT") {
            newSetProduct(20, listOf(newProduct(10)), status = SOLD_OUT).effectiveStatus() shouldBe SOLD_OUT
        }

        it("세트가 SELLING이고 연결 상품 하나가 SOLD_OUT이면 SOLD_OUT") {
            newSetProduct(20, listOf(newProduct(10), newProduct(11, status = SOLD_OUT)))
                .effectiveStatus() shouldBe SOLD_OUT
        }

        it("연결 상품이 HIDDEN이면 품절로 보지 않음") {
            newSetProduct(20, listOf(newProduct(10, status = HIDDEN))).effectiveStatus() shouldBe SELLING
        }

        it("연결 상품이 없으면 세트 status만 봄") {
            newSetProduct(20, listOf(null, null)).effectiveStatus() shouldBe SELLING
        }

        it("계산이 세트의 수동 status를 바꾸지 않음") {
            val set = newSetProduct(20, listOf(newProduct(11, status = SOLD_OUT)))
            set.effectiveStatus()
            set.status shouldBe SELLING
        }
    }
})
