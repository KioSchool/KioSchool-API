package com.kioschool.kioschoolapi.account.service

import com.kioschool.kioschoolapi.domain.account.entity.Account
import com.kioschool.kioschoolapi.domain.account.repository.AccountRepository
import com.kioschool.kioschoolapi.domain.account.service.AccountService
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*

class AccountServiceTest : DescribeSpec({
    val accountRepository = mockk<AccountRepository>()

    val sut = AccountService(accountRepository)

    beforeTest {
        mockkObject(accountRepository)
    }

    afterTest {
        clearAllMocks()
    }

    describe("createAccount") {
        it("should call accountRepository.save") {
            val bank = SampleEntity.bank

            every { accountRepository.save(any<Account>()) } returns SampleEntity.account

            val result = sut.createAccount(bank, "1234", "John Doe")

            assert(result == SampleEntity.account)

            verify { accountRepository.save(any()) }
        }
    }

    describe("deleteAccount") {
        it("should call accountRepository.delete when user has an account") {
            val user = SampleEntity.user.apply { account = SampleEntity.account }

            every { accountRepository.delete(any<Account>()) } just Runs

            sut.deleteAccount(user)

            assert(user.account == null)

            verify { accountRepository.delete(SampleEntity.account) }
        }

        it("should not call accountRepository.delete when user has no account") {
            val user = SampleEntity.user.apply { account = null }

            sut.deleteAccount(user)

            assert(user.account == null)

            verify(exactly = 0) { accountRepository.delete(any<Account>()) }
        }
    }
})