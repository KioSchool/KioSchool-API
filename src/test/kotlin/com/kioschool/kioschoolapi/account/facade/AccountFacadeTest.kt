package com.kioschool.kioschoolapi.account.facade

import com.kioschool.kioschoolapi.domain.account.exception.IncorrectAccountHolderException
import com.kioschool.kioschoolapi.domain.account.facade.AccountFacade
import com.kioschool.kioschoolapi.domain.account.service.AccountService
import com.kioschool.kioschoolapi.domain.account.service.BankService
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.portone.service.PortoneService
import com.kioschool.kioschoolapi.global.toss.exception.DifferentAccountNumberException
import com.kioschool.kioschoolapi.global.toss.service.TossService
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

            assert(result.name == SampleEntity.bank.name)
            assert(result.code == SampleEntity.bank.code)

            verify { bankService.addBank(name, code) }
        }
    }

    describe("deleteBank") {
        it("should call bankService.deleteBank") {
            val id = 1L

            every { bankService.deleteBank(id) } returns SampleEntity.bank

            val result = sut.deleteBank(id)

            assert(result.name == SampleEntity.bank.name)
            assert(result.code == SampleEntity.bank.code)

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
            every { userService.saveUser(any()) } returns SampleEntity.user.apply {
                this.account = SampleEntity.account
            }

            val result = sut.registerAccount(username, bankId, accountNumber, accountHolder)

            assert(result.name == SampleEntity.user.name)
            assert(result.account?.accountNumber == SampleEntity.account.accountNumber)

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
            verify { userService.saveUser(any()) }
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
            every { tossService.removeAmountQueryFromAccountUrl(accountUrl) } returns "removedUrl"
            every { userService.saveUser(any()) } returns SampleEntity.user.apply {
                this.account = SampleEntity.account
            }

            val result = sut.registerTossAccount(username, accountUrl)

            assert(result.name == SampleEntity.user.name)

            verify { userService.getUser(username) }
            verify { tossService.validateAccountUrl(SampleEntity.user, accountUrl) }
            verify { userService.saveUser(any()) }
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

    describe("deleteAccount") {
        it("should call userService.getUser, accountService.deleteAccount, and userService.saveUser and account should be null") {
            val username = "username"
            val user = SampleEntity.user.apply {
                this.account = SampleEntity.account
            }

            every { userService.getUser(username) } returns user
            every { accountService.deleteAccount(user) } returns Unit
            every { userService.saveUser(any()) } returns user.apply {
                this.account = null
            }

            val result = sut.deleteAccount(username)

            assert(result.account == null)

            verify { userService.getUser(username) }
            verify { accountService.deleteAccount(user) }
            verify { userService.saveUser(any()) }
        }
    }

    describe("deleteTossAccount") {
        it("should call userService.getUser, and userService.saveUser and tossAccountUrl should be null") {
            val username = "username"
            val user = SampleEntity.user.apply {
                this.account = SampleEntity.account.apply {
                    this.tossAccountUrl = "some-url"
                }
            }

            every { userService.getUser(username) } returns user
            every { userService.saveUser(any()) } returns user

            val result = sut.deleteTossAccount(username)

            assert(result.account?.tossAccountUrl == null)

            verify { userService.getUser(username) }
            verify { userService.saveUser(any()) }
        }
    }
})