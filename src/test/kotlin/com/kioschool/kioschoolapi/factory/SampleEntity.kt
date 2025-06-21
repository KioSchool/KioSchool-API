package com.kioschool.kioschoolapi.factory

import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.account.entity.Bank
import com.kioschool.kioschoolapi.domain.email.entity.EmailCode
import com.kioschool.kioschoolapi.domain.email.entity.EmailDomain
import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceImage
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceInvitation
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceMember
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.UserRole
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

    val workspaceImage1 = WorkspaceImage(
        workspace = workspace,
        url = "test"
    ).apply { setId(1) }

    val workspaceImage2 = WorkspaceImage(
        workspace = workspace,
        url = "test2"
    ).apply { setId(2) }

    val workspaceImage3 = WorkspaceImage(
        workspace = workspace,
        url = "test3"
    ).apply { setId(3) }

    val workspaceImages = listOf(workspaceImage1, workspaceImage2, workspaceImage3)

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
    ).apply { setId(1) }

    val productCategory2 = ProductCategory(
        name = "test2",
        workspace = workspace
    ).apply { setId(2) }

    val productCategory3 = ProductCategory(
        name = "test3",
        workspace = workspace
    ).apply { setId(3) }

    fun productCategoryWithId(id: Long) = productCategory.apply { setId(id) }

    val productCategories = listOf(productCategory, productCategory2, productCategory3)

    val product = Product(
        name = "test",
        description = "test",
        price = 1000,
        imageUrl = "testImageUrl",
        workspace = workspace,
        productCategory = productCategory
    )

    fun productWithId(id: Long) = product.apply { setId(id) }

    val order1 = Order(
        workspace,
        0,
        "test",
        orderNumber = 1,
        totalPrice = 1000
    )

    val orderProduct1 = OrderProduct(
        order = order1,
        productId = product.id,
        productName = product.name,
        productPrice = product.price,
        quantity = 1
    )

    val order2 = Order(
        workspace,
        0,
        "test",
        orderNumber = 2,
        totalPrice = 3000
    )

    val orderProduct2 = OrderProduct(
        order = order2,
        productId = product.id,
        productName = product.name,
        productPrice = product.price,
        quantity = 3
    )

    val order3 = Order(
        workspace,
        0,
        "test",
        orderNumber = 3,
        totalPrice = 2000
    )

    val orderProduct3 = OrderProduct(
        order = order3,
        productId = product.id,
        productName = product.name,
        productPrice = product.price,
        quantity = 2
    )

    val emailDomain = EmailDomain(
        name = "test",
        domain = "test.com"
    )

    val emailCode = EmailCode(
        email = "test@test.com",
        code = "123456789",
        kind = EmailKind.REGISTER
    )

    val bank = Bank(
        name = "test",
        code = "123"
    )

    val account = Account(
        bank = bank,
        accountNumber = "123456789",
        accountHolder = "test"
    )

    private fun BaseEntity.setId(id: Long) {
        val f = this::class.superclasses.first().java.getDeclaredField("id")
        f.isAccessible = true
        f.set(this, id)
    }
}