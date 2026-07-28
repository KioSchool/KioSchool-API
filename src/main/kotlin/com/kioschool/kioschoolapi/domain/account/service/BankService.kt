package com.kioschool.kioschoolapi.domain.account.service

import com.kioschool.kioschoolapi.domain.account.entity.Bank
import com.kioschool.kioschoolapi.domain.account.repository.BankRepository
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
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
    fun updateTossName(id: Long, tossName: String): Bank {
        val bank = bankRepository.findById(id).orElseThrow { CustomException(ErrorCode.BANK_NOT_FOUND) }
        bank.tossName = tossName
        return bank
    }

    @Transactional
    fun deleteTossName(id: Long): Bank {
        val bank = bankRepository.findById(id).orElseThrow { CustomException(ErrorCode.BANK_NOT_FOUND) }
        bank.tossName = null
        return bank
    }

    @Transactional
    fun deleteBank(id: Long): Bank {
        val bank = bankRepository.findById(id).orElseThrow { CustomException(ErrorCode.BANK_NOT_FOUND) }
        bankRepository.delete(bank)
        return bank
    }

    @Transactional
    fun fillTossNameIfAbsent(bank: Bank, tossName: String) {
        if (bank.tossName != null) return
        bank.tossName = tossName
        bankRepository.save(bank)
    }

    fun getAllBanks(): List<Bank> {
        return bankRepository.findAll()
    }

    fun getBank(bankId: Long): Bank {
        return bankRepository.findById(bankId).orElseThrow { CustomException(ErrorCode.BANK_NOT_FOUND) }
    }
}