package com.kioschool.kioschoolapi.domain.account.dto.common

import com.kioschool.kioschoolapi.domain.account.dto.common.BankDto
import com.kioschool.kioschoolapi.domain.account.entity.Account
import java.time.LocalDateTime

data class AccountDto(
    val id: Long,
    val bank: BankDto,
    val accountNumber: String,
    val accountHolder: String,
    val tossAccountUrl: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(account: Account): AccountDto {
            return AccountDto(
                id = account.id,
                bank = BankDto.of(account.bank),
                accountNumber = account.accountNumber,
                accountHolder = account.accountHolder,
                tossAccountUrl = account.tossAccountUrl,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt
            )
        }
    }
}
