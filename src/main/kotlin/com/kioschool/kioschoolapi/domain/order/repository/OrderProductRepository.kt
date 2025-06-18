package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface OrderProductRepository : JpaRepository<OrderProduct, Long>