package com.kioschool.kioschoolapi.account.service

import com.kioschool.kioschoolapi.domain.account.entity.Bank
import com.kioschool.kioschoolapi.domain.account.exception.BankNotFoundException
import com.kioschool.kioschoolapi.domain.account.repository.BankRepository
import com.kioschool.kioschoolapi.domain.account.service.BankService
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
        it("should call bankRepository.findAll when name is null") {
            val name = null
            val page = 1
            val size = 10

            every { bankRepository.findAll(PageRequest.of(page, size)) } returns
                    PageImpl(
                        listOf(
                            SampleEntity.bank
                        )
                    )

            sut.getBanks(name, page, size)

            verify { bankRepository.findAll(PageRequest.of(page, size)) }
            verify(exactly = 0) {
                bankRepository.findAllByNameContains(name, PageRequest.of(page, size))
            }
        }

        it("should call bankRepository.findAllByNameContains when name is not null") {
            val name = "Bank Name"
            val page = 1
            val size = 10

            every { bankRepository.findAllByNameContains(name, PageRequest.of(page, size)) } returns
                    PageImpl(
                        listOf(
                            SampleEntity.bank
                        )
                    )

            sut.getBanks(name, page, size)

            verify { bankRepository.findAllByNameContains(name, PageRequest.of(page, size)) }
            verify(exactly = 0) {
                bankRepository.findAll(PageRequest.of(page, size))
            }
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

            every { bankRepository.findById(id) } returns Optional.of(SampleEntity.bank)
            every { bankRepository.delete(any<Bank>()) } just Runs

            sut.deleteBank(id)

            verify { bankRepository.findById(id) }
            verify { bankRepository.delete(any<Bank>()) }
        }
    }

    describe("getAllBanks") {
        it("should call bankRepository.findAll") {
            every { bankRepository.findAll() } returns listOf(SampleEntity.bank)

            sut.getAllBanks()

            verify { bankRepository.findAll() }
        }
    }

    describe("updateTossName") {
        it("should update tossName and return bank") {
            val id = 1L
            val tossName = "NH농협은행"
            val bank = SampleEntity.bank

            every { bankRepository.findById(id) } returns Optional.of(bank)

            sut.updateTossName(id, tossName)

            assert(bank.tossName == tossName)
            verify { bankRepository.findById(id) }
        }

        it("should throw BankNotFoundException when bank is not found") {
            val id = 1L

            every { bankRepository.findById(id) } returns Optional.empty()

            assertThrows<BankNotFoundException> {
                sut.updateTossName(id, "NH농협은행")
            }
        }
    }

    describe("deleteTossName") {
        it("should set tossName to null and return bank") {
            val id = 1L
            val bank = SampleEntity.bank.apply { tossName = "NH농협은행" }

            every { bankRepository.findById(id) } returns Optional.of(bank)

            sut.deleteTossName(id)

            assert(bank.tossName == null)
            verify { bankRepository.findById(id) }
        }

        it("should throw BankNotFoundException when bank is not found") {
            val id = 1L

            every { bankRepository.findById(id) } returns Optional.empty()

            assertThrows<BankNotFoundException> {
                sut.deleteTossName(id)
            }
        }
    }

    describe("fillTossNameIfAbsent") {
        it("should fill tossName and call save when tossName is null") {
            val bank = SampleEntity.bank.apply { tossName = null }
            val tossName = "카카오뱅크"

            every { bankRepository.save(bank) } returns bank

            sut.fillTossNameIfAbsent(bank, tossName)

            assert(bank.tossName == tossName)
            verify { bankRepository.save(bank) }
        }

        it("should not call save when tossName is already set") {
            val bank = SampleEntity.bank.apply { tossName = "카카오뱅크" }

            sut.fillTossNameIfAbsent(bank, "다른뱅크")

            assert(bank.tossName == "카카오뱅크")
            verify(exactly = 0) { bankRepository.save(any()) }
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