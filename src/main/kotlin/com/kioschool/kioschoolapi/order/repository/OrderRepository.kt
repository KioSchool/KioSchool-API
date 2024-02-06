package com.kioschool.kioschoolapi.order.repository

import com.kioschool.kioschoolapi.order.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByWorkspaceId(workspaceId: Long): List<Order>
}