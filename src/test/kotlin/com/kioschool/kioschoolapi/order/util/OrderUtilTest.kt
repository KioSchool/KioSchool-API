package com.kioschool.kioschoolapi.order.util

import com.kioschool.kioschoolapi.domain.order.util.OrderUtil
import io.kotest.core.spec.style.DescribeSpec

class OrderUtilTest : DescribeSpec({
    describe("getOrderNumberKey") {
        it("should return order_number_workspaceId") {
            // Arrange
            val workspaceId = 1L

            // Act
            val result = OrderUtil.getOrderNumberKey(workspaceId)

            // Assert
            assert(result == "order_number_$workspaceId")
        }
    }
})