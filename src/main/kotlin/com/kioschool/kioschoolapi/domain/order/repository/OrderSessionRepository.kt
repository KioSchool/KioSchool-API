package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.OrderSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderSessionRepository : JpaRepository<OrderSession, Long>