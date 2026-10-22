package com.kioschool.kioschoolapi.entity

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.OrderBy
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

/**
 * A @OneToMany List with no @OrderBy is a Hibernate bag: no SQL ORDER BY is emitted, and PostgreSQL
 * returns an UPDATEd row last, so editing one row reshuffles the collection. These two are rendered
 * in response order by the admin editor and the customer-facing slider, so the shuffle is visible.
 */
class EntityCollectionOrderingTest : DescribeSpec({
    describe("Workspace collections rendered in API responses") {
        listOf("images", "products").forEach { propertyName ->
            it("orders $propertyName by id so updates cannot reshuffle them") {
                val workspace =
                    Class.forName("com.kioschool.kioschoolapi.domain.workspace.entity.Workspace").kotlin

                val property = workspace.declaredMemberProperties.first { it.name == propertyName }
                val orderBy = property.javaField?.getAnnotation(OrderBy::class.java)

                orderBy?.value shouldBe "id"
            }
        }
    }

    describe("세트 구성품 컬렉션") {
        it("Product.components를 displayOrder 순으로 읽는다") {
            val product = Class.forName("com.kioschool.kioschoolapi.domain.product.entity.Product").kotlin

            val property = product.declaredMemberProperties.first { it.name == "components" }

            property.javaField?.getAnnotation(OrderBy::class.java)?.value shouldBe "displayOrder"
        }

        it("OrderProduct.components를 displayOrder 순으로 읽는다") {
            val orderProduct = Class.forName("com.kioschool.kioschoolapi.domain.order.entity.OrderProduct").kotlin

            val property = orderProduct.declaredMemberProperties.first { it.name == "components" }

            property.javaField?.getAnnotation(OrderBy::class.java)?.value shouldBe "displayOrder"
        }
    }
})
