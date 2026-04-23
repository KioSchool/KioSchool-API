package com.kioschool.kioschoolapi.domain.dashboard.facade

import com.kioschool.kioschoolapi.domain.dashboard.dto.ProductIdQuantityDto
import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.service.OrderService
import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.service.ProductService
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceTable
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import io.mockk.every
import io.mockk.mockk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DashboardFacadeTest {

    private val workspaceService: WorkspaceService = mockk()
    private val orderService: OrderService = mockk()
    private val productService: ProductService = mockk()
    private val dashboardFacade = DashboardFacade(
        workspaceService,
        orderService,
        productService
    )

    @Test
    fun `getDashboard returns aggregated data correctly`() {
        // Given
        val username = "admin"
        val workspaceId = 1L

        // Mock Workspace Entity
        val mockWorkspace = mockk<Workspace>(relaxed = true) {
            every { name } returns "Test Workspace"
            every { notice } returns "Notice"
            every { memo } returns "Memo"
        }

        // Mock Workspace Tables
        val table1 = mockk<WorkspaceTable>(relaxed = true) {
            every { orderSession } returns mockk() // Occupied
        }
        val table2 = mockk<WorkspaceTable>(relaxed = true) {
            every { orderSession } returns null // Empty
        }

        every { workspaceService.getWorkspace(workspaceId) } returns mockWorkspace
        every { workspaceService.getAllWorkspaceTables(mockWorkspace) } returns listOf(
            table1,
            table2
        )

        // Mock OrderService (Stats)
        every { orderService.getSalesSum(workspaceId, any(), any()) } returns 10000L
        every { orderService.getOrderCount(workspaceId, any(), any()) } returns 2L

        // Mock Top Selling
        every { orderService.getTopSellingProducts(workspaceId, any(), any(), 5) } returns listOf(
            ProductIdQuantityDto(10L, "Pizza", 10)
        )

        val pizzaProducts = listOf(
            mockk<Product>(relaxed = true) {
                every { id } returns 10L
                every { name } returns "Pizza"
            }
        )

        every { productService.getProducts(listOf(10L)) } returns pizzaProducts

        // Mock Recent Orders (Returns Entities)
        val mockOrder = mockk<Order>(relaxed = true) {
            every { id } returns 100L
            every { tableNumber } returns 1
            every { totalPrice } returns 5000
            every { status } returns OrderStatus.NOT_PAID
            every { createdAt } returns LocalDateTime.now()
            every { customerName } returns "TestCustomer"
            every { orderNumber } returns 1L
            // Relaxed mock handles collections/other fields if accessed by OrderDto.of
        }
        every {
            orderService.getAllOrdersByCondition(
                workspaceId,
                any(),
                any(),
                any(),
                any()
            )
        } returns listOf(mockOrder)

        // Mock Out of Stock Products (Returns Entities)
        val soldOutProduct = mockk<Product>(relaxed = true) {
            every { id } returns 10L
            every { name } returns "SoldOutItem"
            every { status } returns ProductStatus.SOLD_OUT
        }
        // ProductService usually returns all products, Facade filters. 
        // Logic: productService.getAllProductsByCondition(workspaceId).filter { SOLD_OUT }
        every { productService.getAllProductsByCondition(workspaceId) } returns listOf(
            soldOutProduct
        )

        // When
        val result = dashboardFacade.getDashboard(username, workspaceId)

        // Then
        // Workspace
        assertEquals("Test Workspace", result.dashboardWorkspaceInfo.name)
        assertEquals(1, result.dashboardWorkspaceInfo.occupiedTables)
        assertEquals(2, result.dashboardWorkspaceInfo.totalTables)

        // Stats
        assertEquals(10000L, result.stats.totalSales)
        assertEquals(2L, result.stats.totalOrderCount)
        assertEquals(5000.0, result.stats.averageOrderAmount)

        // Top Selling
        assertEquals(1, result.topSellingProducts.size)
        assertEquals("Pizza", result.topSellingProducts[0].product.name)

        // Recent Orders
        assertEquals(1, result.recentOrders.size)
        assertEquals(100L, result.recentOrders[0].id)

        // Out of Stock
        assertEquals(1, result.outOfStockProducts.size)
        assertEquals("SoldOutItem", result.outOfStockProducts[0].name)
    }
}
