package com.kioschool.kioschoolapi.account.repository

import com.kioschool.kioschoolapi.account.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<Account, Long>