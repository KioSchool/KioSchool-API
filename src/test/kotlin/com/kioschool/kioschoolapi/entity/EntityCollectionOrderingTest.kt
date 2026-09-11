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
})
