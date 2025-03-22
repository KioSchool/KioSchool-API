package com.kioschool.kioschoolapi.order.util

class OrderUtil {
    companion object {
        fun getOrderNumberKey(workspaceId: Long): String {
            return "order_number_$workspaceId"
        }
    }
}