package com.kioschool.kioschoolapi.order.repository

import com.kioschool.kioschoolapi.order.entity.OrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface OrderProductRepository : JpaRepository<OrderProduct, Long>