package com.kioschool.kioschoolapi.account.facade

import com.kioschool.kioschoolapi.domain.account.facade.AccountFacade
import com.kioschool.kioschoolapi.domain.account.service.AccountService
import com.kioschool.kioschoolapi.domain.account.service.BankService
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.portone.service.PortoneService
import com.kioschool.kioschoolapi.global.toss.service.TossService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl

class AccountFacadeTest : DescribeSpec({
    val bankService = mockk<BankService>()
    val accountService = mockk<AccountService>()
    val userService = mockk<UserService>()
    val userRepository = mockk<UserRepository>()
    val portoneService = mockk<PortoneService>()
    val tossService = mockk<TossService>()

    val sut = AccountFacade(bankService, accountService, userService, userRepository, portoneService, tossService)

    beforeTest {
        mockkObject(bankService)
        mockkObject(accountService)
        mockkObject(userService)
        mockkObject(userRepository)
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
        it("PortOne 조회값을 예금주명으로 저장한다") {
            val username = "username"
            val bankId = 1L
            val accountNumber = "accountNumber"
            val realAccountHolder = "박지인(모임통장)"

            every { bankService.getBank(bankId) } returns SampleEntity.bank
            every {
                portoneService.getAccountHolder(SampleEntity.bank.code, accountNumber)
            } returns realAccountHolder
            every { userService.getUser(username) } returns SampleEntity.user
            every {
                accountService.createAccount(
                    SampleEntity.bank,
                    accountNumber,
                    realAccountHolder
                )
            } returns SampleEntity.account
            every { userService.saveUser(any()) } returns SampleEntity.user.apply {
                this.account = SampleEntity.account
            }

            val result = sut.registerAccount(username, bankId, accountNumber)

            assert(result.name == SampleEntity.user.name)
            assert(result.account?.accountNumber == SampleEntity.account.accountNumber)

            verify { bankService.getBank(bankId) }
            verify { portoneService.getAccountHolder(SampleEntity.bank.code, accountNumber) }
            verify { userService.getUser(username) }
            verify {
                accountService.createAccount(
                    SampleEntity.bank,
                    accountNumber,
                    realAccountHolder
                )
            }
            verify { userService.saveUser(any()) }
        }

        it("PortOne 조회에 실패하면 계좌를 저장하지 않는다") {
            val username = "username"
            val bankId = 1L
            val accountNumber = "accountNumber"

            every { bankService.getBank(bankId) } returns SampleEntity.bank
            every {
                portoneService.getAccountHolder(SampleEntity.bank.code, accountNumber)
            } throws CustomException(ErrorCode.ACCOUNT_HOLDER_NOT_FOUND)

            val ex = assertThrows<CustomException> {
                sut.registerAccount(username, bankId, accountNumber)
            }
            assertEquals(ErrorCode.ACCOUNT_HOLDER_NOT_FOUND, ex.errorCode)

            verify { bankService.getBank(bankId) }
            verify { portoneService.getAccountHolder(SampleEntity.bank.code, accountNumber) }
            verify(exactly = 0) { userService.getUser(username) }
            verify(exactly = 0) { accountService.createAccount(any(), any(), any()) }
            verify(exactly = 0) { userService.saveUser(any()) }
        }
    }

    describe("registerTossAccount") {
        it("should call userService.getUser, tossService.validateAccountUrl, userService.saveUser") {
            val username = "username"
            val accountUrl = "accountUrl"

            every { userService.getUser(username) } returns SampleEntity.user
            every { tossService.validateAccountUrl(SampleEntity.user, accountUrl) } returns Unit
            every { tossService.removeAmountQueryFromAccountUrl(accountUrl) } returns "removedUrl"
            every { tossService.extractBankNameFromUrl(accountUrl) } returns null
            every { userService.saveUser(any()) } returns SampleEntity.user.apply {
                this.account = SampleEntity.account
            }

            val result = sut.registerTossAccount(username, accountUrl)

            assert(result.name == SampleEntity.user.name)

            verify { userService.getUser(username) }
            verify { tossService.validateAccountUrl(SampleEntity.user, accountUrl) }
            verify { userService.saveUser(any()) }
        }

        it("should auto-fill bank tossName when bank has no tossName") {
            val username = "username"
            val accountUrl = "supertoss://send?bank=%EC%B9%B4%EC%B9%B4%EC%98%A4%EB%B1%85%ED%81%AC&accountNo=3333280467267&origin=qr"
            val userWithAccount = SampleEntity.user.apply {
                this.account = SampleEntity.account.apply { bank.tossName = null }
            }

            every { userService.getUser(username) } returns userWithAccount
            every { tossService.validateAccountUrl(userWithAccount, accountUrl) } returns Unit
            every { tossService.removeAmountQueryFromAccountUrl(accountUrl) } returns accountUrl
            every { tossService.extractBankNameFromUrl(accountUrl) } returns "카카오뱅크"
            every { bankService.fillTossNameIfAbsent(SampleEntity.bank, "카카오뱅크") } just Runs
            every { userService.saveUser(any()) } returns userWithAccount

            sut.registerTossAccount(username, accountUrl)

            verify { tossService.extractBankNameFromUrl(accountUrl) }
            verify { bankService.fillTossNameIfAbsent(SampleEntity.bank, "카카오뱅크") }
        }

        it("should call fillTossNameIfAbsent even when bank already has tossName") {
            val username = "username"
            val accountUrl = "supertoss://send?bank=%EC%B9%B4%EC%B9%B4%EC%98%A4%EB%B1%85%ED%81%AC&accountNo=3333280467267&origin=qr"
            val userWithAccount = SampleEntity.user.apply {
                this.account = SampleEntity.account.apply { bank.tossName = "카카오뱅크" }
            }

            every { userService.getUser(username) } returns userWithAccount
            every { tossService.validateAccountUrl(userWithAccount, accountUrl) } returns Unit
            every { tossService.removeAmountQueryFromAccountUrl(accountUrl) } returns accountUrl
            every { tossService.extractBankNameFromUrl(accountUrl) } returns "카카오뱅크"
            every { bankService.fillTossNameIfAbsent(SampleEntity.bank, "카카오뱅크") } just Runs
            every { userService.saveUser(any()) } returns userWithAccount

            sut.registerTossAccount(username, accountUrl)

            // fillTossNameIfAbsent는 항상 호출되며, 내부에서 tossName 유무를 판단함
            verify { bankService.fillTossNameIfAbsent(SampleEntity.bank, "카카오뱅크") }
        }

        it("should throw CustomException when account number does not match") {
            val username = "username"
            val accountUrl = "accountUrl"

            every { userService.getUser(username) } returns SampleEntity.user
            every {
                tossService.validateAccountUrl(SampleEntity.user, accountUrl)
            } throws CustomException(ErrorCode.DIFFERENT_ACCOUNT_NUMBER)

            val ex = assertThrows<CustomException> {
                sut.registerTossAccount(username, accountUrl)
            }
            assertEquals(ErrorCode.DIFFERENT_ACCOUNT_NUMBER, ex.errorCode)

            verify { userService.getUser(username) }
            verify { tossService.validateAccountUrl(SampleEntity.user, accountUrl) }
            verify(exactly = 0) { userService.saveUser(SampleEntity.user) }
        }
    }

    describe("registerTossAccountAuto") {
        it("should generate and save toss url when bank has tossName") {
            val username = "username"
            val userWithAccount = SampleEntity.user.apply {
                this.account = SampleEntity.account.apply {
                    bank.tossName = "카카오뱅크"
                    accountNumber = "3333280467267"
                }
            }
            val generatedUrl = "supertoss://send?bank=%EC%B9%B4%EC%B9%B4%EC%98%A4%EB%B1%85%ED%81%AC&accountNo=3333280467267&origin=qr"

            every { userService.getUser(username) } returns userWithAccount
            every { tossService.generateTossAccountUrl("카카오뱅크", "3333280467267") } returns generatedUrl
            every { userService.saveUser(any()) } returns userWithAccount

            sut.registerTossAccountAuto(username)

            assert(userWithAccount.account?.tossAccountUrl == generatedUrl)
            verify { tossService.generateTossAccountUrl("카카오뱅크", "3333280467267") }
            verify { userService.saveUser(any()) }
        }

        it("should throw CustomException when bank has no tossName") {
            val username = "username"
            val userWithAccount = SampleEntity.user.apply {
                this.account = SampleEntity.account.apply { bank.tossName = null }
            }

            every { userService.getUser(username) } returns userWithAccount

            val ex = assertThrows<CustomException> {
                sut.registerTossAccountAuto(username)
            }
            assertEquals(ErrorCode.BANK_TOSS_NAME_NOT_FOUND, ex.errorCode)

            verify(exactly = 0) { tossService.generateTossAccountUrl(any(), any()) }
            verify(exactly = 0) { userService.saveUser(any()) }
        }

        it("should throw IllegalStateException when account is not registered") {
            val username = "username"
            val userWithoutAccount = SampleEntity.user.apply { this.account = null }

            every { userService.getUser(username) } returns userWithoutAccount

            assertThrows<IllegalStateException> {
                sut.registerTossAccountAuto(username)
            }

            verify(exactly = 0) { tossService.generateTossAccountUrl(any(), any()) }
            verify(exactly = 0) { userService.saveUser(any()) }
        }
    }

    describe("updateBankTossName") {
        it("should call bankService.updateTossName") {
            val id = 1L
            val tossName = "NH농협은행"

            every { bankService.updateTossName(id, tossName) } returns SampleEntity.bank

            sut.updateBankTossName(id, tossName)

            verify { bankService.updateTossName(id, tossName) }
        }
    }

    describe("deleteBankTossName") {
        it("should call bankService.deleteTossName") {
            val id = 1L

            every { bankService.deleteTossName(id) } returns SampleEntity.bank

            sut.deleteBankTossName(id)

            verify { bankService.deleteTossName(id) }
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