package com.kioschool.kioschoolapi.account.service

import com.kioschool.kioschoolapi.account.entity.Bank
import com.kioschool.kioschoolapi.account.exception.BankNotFoundException
import com.kioschool.kioschoolapi.account.repository.BankRepository
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class BankService(
    private val bankRepository: BankRepository
) {
    fun getBanks(name: String?, page: Int, size: Int): Page<Bank> {
        if (!name.isNullOrBlank()) {
            return bankRepository.findAllByNameContains(name, PageRequest.of(page, size))
        }

        return bankRepository.findAll(PageRequest.of(page, size))
    }

    fun addBank(name: String, code: String): Bank {
        return bankRepository.save(Bank(name = name, code = code))
    }

    @Transactional
    fun deleteBank(id: Long): Bank {
        val bank = bankRepository.findById(id).orElseThrow { BankNotFoundException() }
        bankRepository.delete(bank)
        return bank
    }

    fun getAllBanks(): List<Bank> {
        return bankRepository.findAll()
    }

    fun getBank(bankId: Long): Bank {
        return bankRepository.findById(bankId).orElseThrow { BankNotFoundException() }
    }
}