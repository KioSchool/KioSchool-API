package com.kioschool.kioschoolapi.factory

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.product.entity.Product
import com.kioschool.kioschoolapi.product.entity.ProductCategory
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.workspace.entity.Workspace
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceInvitation
import com.kioschool.kioschoolapi.workspace.entity.WorkspaceMember
import kotlin.reflect.full.superclasses

object SampleEntity {
    val user = User(
        loginId = "test",
        loginPassword = "test",
        name = "test",
        email = "test@test.com",
        role = UserRole.ADMIN,
        accountUrl = "test",
        members = mutableListOf()
    )

    val otherUser = User(
        loginId = "test2",
        loginPassword = "test2",
        name = "test2",
        email = "test2@other.com",
        role = UserRole.ADMIN,
        accountUrl = "test2",
        members = mutableListOf()
    )

    fun userWithId(id: Long) = user.apply { setId(id) }

    val workspace = Workspace(
        name = "test",
        owner = user
    )

    fun workspace(user: User) = Workspace(
        name = "test",
        owner = user
    )

    val workspaceInvitation = WorkspaceInvitation(
        workspace = workspace,
        user = user
    )

    fun workspaceInvitation(user: User, workspace: Workspace) = WorkspaceInvitation(
        workspace = workspace,
        user = user
    )

    val workspaceMember = WorkspaceMember(
        workspace = workspace,
        user = user
    )

    fun workspaceMember(user: User, workspace: Workspace) = WorkspaceMember(
        workspace = workspace,
        user = user
    )

    fun workspaceWithId(id: Long) = workspace.apply { setId(id) }

    val productCategory = ProductCategory(
        name = "test",
        workspace = workspace
    )

    fun productCategoryWithId(id: Long) = productCategory.apply { setId(id) }

    val product = Product(
        name = "test",
        description = "test",
        price = 1000,
        imageUrl = "testImgaeUrl",
        workspace = workspace,
        productCategory = productCategory
    )

    fun productWithId(id: Long) = product.apply { setId(id) }

    val order = Order(
        workspace,
        0,
        "test"
    )

    val orderProduct = OrderProduct(
        order = order,
        productId = product.id,
        productName = product.name,
        productPrice = product.price,
        quantity = 1
    )

    private fun BaseEntity.setId(id: Long) {
        val f = this::class.superclasses.first().java.getDeclaredField("id")
        f.isAccessible = true
        f.set(this, id)
    }
}