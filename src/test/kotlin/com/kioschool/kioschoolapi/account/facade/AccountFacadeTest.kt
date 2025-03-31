package com.kioschool.kioschoolapi.account.facade

import com.kioschool.kioschoolapi.account.exception.IncorrectAccountHolderException
import com.kioschool.kioschoolapi.account.service.AccountService
import com.kioschool.kioschoolapi.account.service.BankService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.portone.service.PortoneService
import com.kioschool.kioschoolapi.toss.exception.DifferentAccountNumberException
import com.kioschool.kioschoolapi.toss.service.TossService
import com.kioschool.kioschoolapi.user.service.UserService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl

class AccountFacadeTest : DescribeSpec({
    val bankService = mockk<BankService>()
    val accountService = mockk<AccountService>()
    val userService = mockk<UserService>()
    val portoneService = mockk<PortoneService>()
    val tossService = mockk<TossService>()

    val sut = AccountFacade(bankService, accountService, userService, portoneService, tossService)

    beforeTest {
        mockkObject(bankService)
        mockkObject(accountService)
        mockkObject(userService)
        mockkObject(portoneService)
        mockkObject(tossService)
    }

    afterTest {
        clearAllMocks()
    }

    describe("getBanks") {
        it("should call bankService.getBanks") {
            val name = "name"
            val page = 1
            val size = 10

            every {
                bankService.getBanks(
                    name,
                    page,
                    size
                )
            } returns PageImpl(emptyList())

            sut.getBanks(name, page, size)

            verify { bankService.getBanks(name, page, size) }
        }
    }

    describe("getAllBanks") {
        it("should call bankService.getAllBanks") {
            every {
                bankService.getAllBanks()
            } returns emptyList()

            sut.getAllBanks()

            verify { bankService.getAllBanks() }
        }
    }

    describe("addBank") {
        it("should call bankService.addBank") {
            val name = "name"
            val code = "code"

            every {
                bankService.addBank(
                    name,
                    code
                )
            } returns SampleEntity.bank

            val result = sut.addBank(name, code)

            assert(result == SampleEntity.bank)

            verify { bankService.addBank(name, code) }
        }
    }

    describe("deleteBank") {
        it("should call bankService.deleteBank") {
            val id = 1L

            every { bankService.deleteBank(id) } returns SampleEntity.bank

            sut.deleteBank(id)

            verify { bankService.deleteBank(id) }
        }
    }

    describe("registerAccount") {
        it("should call bankService.getBank, portoneService.validateAccountHolder, userService.getUser, accountService.createAccount, userService.saveUser") {
            val username = "username"
            val bankId = 1L
            val accountNumber = "accountNumber"
            val accountHolder = "accountHolder"

            every { bankService.getBank(bankId) } returns SampleEntity.bank
            every {
                portoneService.validateAccountHolder(
                    SampleEntity.bank.code,
                    accountNumber,
                    accountHolder
                )
            } returns Unit
            every { userService.getUser(username) } returns SampleEntity.user
            every {
                accountService.createAccount(
                    SampleEntity.bank,
                    accountNumber,
                    accountHolder
                )
            } returns SampleEntity.account
            every { userService.saveUser(SampleEntity.user) } returns SampleEntity.user

            val result = sut.registerAccount(username, bankId, accountNumber, accountHolder)

            assert(result == SampleEntity.user)
            assert(result.account == SampleEntity.account)

            verify { bankService.getBank(bankId) }
            verify {
                portoneService.validateAccountHolder(
                    SampleEntity.bank.code,
                    accountNumber,
                    accountHolder
                )
            }
            verify { userService.getUser(username) }
            verify { accountService.createAccount(SampleEntity.bank, accountNumber, accountHolder) }
            verify { userService.saveUser(SampleEntity.user) }
        }

        it("should throw IncorrectAccountHolderException when account holder is incorrect") {
            val username = "username"
            val bankId = 1L
            val accountNumber = "accountNumber"
            val accountHolder = "accountHolder"

            every { bankService.getBank(bankId) } returns SampleEntity.bank
            every {
                portoneService.validateAccountHolder(
                    SampleEntity.bank.code,
                    accountNumber,
                    accountHolder
                )
            } throws IncorrectAccountHolderException()

            assertThrows<IncorrectAccountHolderException> {
                sut.registerAccount(username, bankId, accountNumber, accountHolder)
            }

            verify { bankService.getBank(bankId) }
            verify {
                portoneService.validateAccountHolder(
                    SampleEntity.bank.code,
                    accountNumber,
                    accountHolder
                )
            }
            verify(exactly = 0) { userService.getUser(username) }
            verify(exactly = 0) {
                accountService.createAccount(
                    SampleEntity.bank,
                    accountNumber,
                    accountHolder
                )
            }
            verify(exactly = 0) { userService.saveUser(SampleEntity.user) }
        }
    }

    describe("registerTossAccount") {
        it("should call userService.getUser, tossService.validateAccountUrl, userService.saveUser") {
            val username = "username"
            val accountUrl = "accountUrl"

            every { userService.getUser(username) } returns SampleEntity.user
            every { tossService.validateAccountUrl(SampleEntity.user, accountUrl) } returns Unit
            every { userService.saveUser(SampleEntity.user) } returns SampleEntity.user

            val result = sut.registerTossAccount(username, accountUrl)

            assert(result == SampleEntity.user)

            verify { userService.getUser(username) }
            verify { tossService.validateAccountUrl(SampleEntity.user, accountUrl) }
            verify { userService.saveUser(SampleEntity.user) }
        }

        it("should throw DifferentAccountNumberException when account holder is incorrect") {
            val username = "username"
            val accountUrl = "accountUrl"

            every { userService.getUser(username) } returns SampleEntity.user
            every {
                tossService.validateAccountUrl(
                    SampleEntity.user,
                    accountUrl
                )
            } throws DifferentAccountNumberException()

            assertThrows<DifferentAccountNumberException> {
                sut.registerTossAccount(username, accountUrl)
            }

            verify { userService.getUser(username) }
            verify { tossService.validateAccountUrl(SampleEntity.user, accountUrl) }
            verify(exactly = 0) { userService.saveUser(SampleEntity.user) }
        }
    }
})