package com.kioschool.kioschoolapi.account.service

import com.kioschool.kioschoolapi.account.entity.Bank
import com.kioschool.kioschoolapi.account.exception.BankNotFoundException
import com.kioschool.kioschoolapi.account.repository.BankRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*

class BankServiceTest : DescribeSpec({
    val bankRepository = mockk<BankRepository>()

    val sut = BankService(bankRepository)

    beforeTest {
        mockkObject(bankRepository)
    }

    afterTest {
        clearAllMocks()
    }

    describe("getBanks") {
        it("should call bankRepository.findAll") {
            val page = 1
            val size = 10

            every { bankRepository.findAll(PageRequest.of(page, size)) } returns
                    PageImpl(
                        listOf(
                            SampleEntity.bank
                        )
                    )

            sut.getBanks(page, size)

            verify { bankRepository.findAll(PageRequest.of(page, size)) }
        }
    }

    describe("addBank") {
        it("should call bankRepository.save") {
            val name = "Bank Name"
            val code = "1234"

            every { bankRepository.save(any<Bank>()) } returns SampleEntity.bank

            sut.addBank(name, code)

            verify { bankRepository.save(any<Bank>()) }
        }
    }

    describe("deleteBank") {
        it("should call bankRepository.deleteById") {
            val id = 1L

            every { bankRepository.deleteById(id) } just Runs

            sut.deleteBank(id)

            verify { bankRepository.deleteById(id) }
        }
    }

    describe("getAllBanks") {
        it("should call bankRepository.findAll") {
            every { bankRepository.findAll() } returns listOf(SampleEntity.bank)

            sut.getAllBanks()

            verify { bankRepository.findAll() }
        }
    }

    describe("getBank") {
        it("should call bankRepository.findById") {
            val bankId = 1L

            every { bankRepository.findById(bankId) } returns Optional.of(SampleEntity.bank)

            sut.getBank(bankId)

            verify { bankRepository.findById(bankId) }
        }

        it("should throw BankNotFoundException when bank is not found") {
            val bankId = 1L

            every { bankRepository.findById(bankId) } returns Optional.empty()

            assertThrows<BankNotFoundException> {
                sut.getBank(bankId)
            }

            verify { bankRepository.findById(bankId) }
        }
    }
})