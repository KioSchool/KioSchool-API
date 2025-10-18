package com.kioschool.kioschoolapi.domain.account.dto

import com.kioschool.kioschoolapi.domain.account.entity.Bank
import java.time.LocalDateTime

data class BankDto(
    val id: Long,
    val name: String,
    val code: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(bank: Bank): BankDto {
            return BankDto(
                id = bank.id,
                name = bank.name,
                code = bank.code,
                createdAt = bank.createdAt,
                updatedAt = bank.updatedAt
            )
        }
    }
}
