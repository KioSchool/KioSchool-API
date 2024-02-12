package com.kioschool.kioschoolapi.bank.repository

import com.kioschool.kioschoolapi.bank.entity.BankCode
import org.springframework.data.jpa.repository.JpaRepository

interface BankCodeRepository : JpaRepository<BankCode, Long> {
    fun findByName(name: String): BankCode?
}