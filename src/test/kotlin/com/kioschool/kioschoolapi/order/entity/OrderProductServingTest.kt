package com.kioschool.kioschoolapi.order.entity

import com.kioschool.kioschoolapi.factory.SampleEntity.newSetOrderProduct
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class OrderProductServingTest : DescribeSpec({
    describe("세트 주문 항목 서빙 수") {
        it("구성품별 floor(served / quantity)의 최솟값") {
            val op = newSetOrderProduct(1, quantity = 2, componentQuantities = listOf(1, 2))
            op.components[0].servedCount = 2
            op.components[1].servedCount = 3 // floor(3/2) = 1
            op.recalculateServedCountFromComponents()
            op.servedCount shouldBe 1
            op.isServed shouldBe false
        }

        it("모든 구성품이 다 나가면 isServed") {
            val op = newSetOrderProduct(1, quantity = 2, componentQuantities = listOf(1, 2))
            op.components[0].servedCount = 2
            op.components[1].servedCount = 4
            op.recalculateServedCountFromComponents()
            op.servedCount shouldBe 2
            op.isServed shouldBe true
        }

        it("serveAllComponents(n)은 구성품을 quantity × n으로 맞춤") {
            val op = newSetOrderProduct(1, quantity = 2, componentQuantities = listOf(1, 2))
            op.serveAllComponents(2)
            op.components.map { it.servedCount } shouldBe listOf(2, 4)
            op.servedCount shouldBe 2
            op.isServed shouldBe true

            op.serveAllComponents(0)
            op.components.map { it.servedCount } shouldBe listOf(0, 0)
            op.servedCount shouldBe 0
            op.isServed shouldBe false
        }

        it("totalQuantity = 구성품 수량 × 세트 주문 수량") {
            newSetOrderProduct(1, quantity = 3, componentQuantities = listOf(2)).components[0].totalQuantity() shouldBe 6
        }

        it("구성품이 있으면 세트 항목") {
            newSetOrderProduct(1, quantity = 1, componentQuantities = listOf(1)).isSet() shouldBe true
            newSetOrderProduct(1, quantity = 1, componentQuantities = emptyList()).isSet() shouldBe false
        }
    }
})
