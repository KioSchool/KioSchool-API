package com.kioschool.kioschoolapi.entity

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import jakarta.persistence.OrderBy
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

/**
 * A @OneToMany List with no @OrderBy is a Hibernate *bag*: the collection is materialised in
 * whatever order the database happens to return rows, and no SQL ORDER BY is emitted. PostgreSQL
 * has no stable row order either -- an UPDATE writes a new tuple version, which a sequential scan
 * then returns last -- so simply editing one row reshuffles the collection.
 *
 * Workspace.images is serialised straight into WorkspaceDto.images and rendered in that order by
 * both the admin editor and the customer-facing slider, so the shuffle is user-visible: changing
 * one photo's focal point moved that photo to the end of the list. Workspace.products already
 * carried @OrderBy("id") for the same reason.
 *
 * WorkspaceOgImageListener independently works around the missing order with
 * `images.minByOrNull { it.id }`, which is the same "oldest image first" rule @OrderBy("id")
 * states declaratively.
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
