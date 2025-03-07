package com.kioschool.kioschoolapi.account.service

import com.kioschool.kioschoolapi.account.entity.Bank
import com.kioschool.kioschoolapi.account.exception.BankNotFoundException
import com.kioschool.kioschoolapi.account.repository.BankRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class BankService(
    private val bankRepository: BankRepository
) {
    fun getBanks(page: Int, size: Int): Page<Bank> {
        return bankRepository.findAll(PageRequest.of(page, size))
    }

    fun addBank(name: String, code: String): Bank {
        return bankRepository.save(Bank(name = name, code = code))
    }

    fun deleteBank(id: Long) {
        bankRepository.deleteById(id)
    }

    fun getAllBanks(): List<Bank> {
        return bankRepository.findAll()
    }

    fun getBank(bankId: Long): Bank {
        return bankRepository.findById(bankId).orElseThrow { BankNotFoundException() }
    }
}