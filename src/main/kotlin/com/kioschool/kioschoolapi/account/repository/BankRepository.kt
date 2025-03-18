package com.kioschool.kioschoolapi.account.repository

import com.kioschool.kioschoolapi.account.entity.Bank
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BankRepository : JpaRepository<Bank, Long>