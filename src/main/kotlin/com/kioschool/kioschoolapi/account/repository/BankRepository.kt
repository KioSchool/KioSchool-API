package com.kioschool.kioschoolapi.account.repository

import com.kioschool.kioschoolapi.account.entity.Bank
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BankRepository : JpaRepository<Bank, Long> {
    fun findAllByNameContains(name: String?, pageable: Pageable): Page<Bank>
}