package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.user.exception.NoPermissionException
import com.kioschool.kioschoolapi.user.exception.RegisterException
import com.kioschool.kioschoolapi.user.exception.UserNotFoundException
import com.kioschool.kioschoolapi.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest : DescribeSpec({
    val repository = mockk<UserRepository>()
    val passwordEncoder = mockk<PasswordEncoder>()
    val emailService = mockk<EmailService>()

    val sut = UserService(
        repository,
        passwordEncoder,
        emailService
    )

    describe("checkPassword") {
        it("should throw LoginFailedException when password is not matched") {
            // Given
            val user = User(
                loginId = "test",
                loginPassword = "test",
                name = "test",
                email = "test@test.com",
                role = UserRole.ADMIN,
                accountUrl = "test",
                members = mutableListOf()
            )
            val loginPassword = "wrong password"

            // Mock
            every { passwordEncoder.matches(loginPassword, user.loginPassword) } returns false

            // Act & Assert
            shouldThrow<LoginFailedException> {
                sut.checkPassword(user, loginPassword)
            }
        }

        it("should not throw LoginFailedException when password is matched") {
            // Given
            val user = User(
                loginId = "test",
                loginPassword = "test",
                name = "test",
                email = "test@test.com",
                role = UserRole.ADMIN,
                accountUrl = "test",
                members = mutableListOf()
            )
            val loginPassword = "test"

            // Mock
            every { passwordEncoder.matches(loginPassword, user.loginPassword) } returns true

            // Act & Assert
            sut.checkPassword(user, loginPassword)
        }
    }

    describe("saveUser") {
        it("should save user") {
            val user = SampleEntity.user

            // Mock
            every { repository.save(user) } returns user

            // Act
            sut.saveUser(user)

            // Assert
            verify { repository.save(user) }
        }
    }

    describe("saveUser with parameters") {
        it("should save user") {
            val loginId = "test"
            val loginPassword = "test"
            val name = "test"
            val email = "test@test.com"

            // Mock
            every { repository.save(any<User>()) } returns SampleEntity.user
            every { passwordEncoder.encode(loginPassword) } returns "encoded password"

            // Act
            sut.saveUser(loginId, loginPassword, name, email) shouldBe SampleEntity.user

            // Assert
            verify { repository.save(any<User>()) }
        }
    }

    describe("validateLoginId") {
        it("should throw RegisterException when loginId is duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            shouldThrow<RegisterException> {
                sut.validateLoginId(loginId)
            }
        }

        it("should not throw RegisterException when loginId is not duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            sut.validateLoginId(loginId)
        }
    }

    describe("validateEmail") {
        it("should throw RegisterException when email is not verified") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns false

            // Act & Assert
            shouldThrow<RegisterException> {
                sut.validateEmail(email)
            }
        }

        it("should throw RegisterException when email is duplicated") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returns SampleEntity.user

            // Act & Assert
            shouldThrow<RegisterException> {
                sut.validateEmail(email)
            }
        }

        it("should not throw RegisterException when email is verified and not duplicated") {
            val email = "test@test.com"

            // Mock
            every { emailService.isRegisterEmailVerified(email) } returns true
            every { repository.findByEmail(email) } returns null

            // Act & Assert
            sut.validateEmail(email)
        }
    }

    describe("isDuplicateLoginId") {
        it("should return true when loginId is duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            sut.isDuplicateLoginId(loginId) shouldBe true
        }

        it("should return false when loginId is not duplicated") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            sut.isDuplicateLoginId(loginId) shouldBe false
        }
    }

    describe("getUser") {
        it("should return user when user exists") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns SampleEntity.user

            // Act & Assert
            sut.getUser(loginId) shouldBe SampleEntity.user
        }

        it("should throw UserNotFoundException when user does not exist") {
            val loginId = "test"

            // Mock
            every { repository.findByLoginId(loginId) } returns null

            // Act & Assert
            shouldThrow<UserNotFoundException> {
                sut.getUser(loginId)
            }
        }
    }

    describe("getUserByEmail") {
        it("should return user when user exists") {
            val email = "test@test.com"

            // Mock
            every { repository.findByEmail(email) } returns SampleEntity.user

            // Act & Assert
            sut.getUserByEmail(email) shouldBe SampleEntity.user

        }

        it("should throw UserNotFoundException when user does not exist") {
            val email = "test@test.com"

            // Mock
            every { repository.findByEmail(email) } returns null

            // Act & Assert
            shouldThrow<UserNotFoundException> {
                sut.getUserByEmail(email)
            }
        }
    }

    describe("getAllUsers") {
        it("should call findByNameContains if name is not null") {
            val name = "test"
            val page = 0
            val size = 10

            // Mock
            every { repository.findByNameContains(name, Pageable.ofSize(size)) } returns PageImpl(
                listOf(SampleEntity.user)
            )

            // Act
            sut.getAllUsers(name, page, size)

            // Assert
            verify { repository.findByNameContains(name, Pageable.ofSize(size)) }
        }

        it("should call findAll if name is null") {
            val name = null
            val page = 0
            val size = 10

            // Mock
            every { repository.findAll(Pageable.ofSize(size)) } returns PageImpl(listOf(SampleEntity.user))

            // Act
            sut.getAllUsers(name, page, size)

            // Assert
            verify { repository.findAll(Pageable.ofSize(size)) }
        }
    }

    describe("isSuperAdminUser") {
        it("should return true when user is super admin") {
            val username = "test"
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            // Mock
            every { repository.findByLoginId(username) } returns user

            // Act & Assert
            sut.isSuperAdminUser(username) shouldBe true
        }

        it("should return false when user is not super admin") {
            val username = "test"
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            // Mock
            every { repository.findByLoginId(username) } returns user

            // Act & Assert
            sut.isSuperAdminUser(username) shouldBe false
        }
    }

    describe("checkHasSuperAdminPermission") {
        it("should not throw exception when user is super admin") {
            val user = SampleEntity.user
            user.role = UserRole.SUPER_ADMIN

            // Act & Assert
            sut.checkHasSuperAdminPermission(user)
        }

        it("should throw exception when user is not super admin") {
            val user = SampleEntity.user
            user.role = UserRole.ADMIN

            // Act & Assert
            shouldThrow<NoPermissionException> {
                sut.checkHasSuperAdminPermission(user)
            }
        }
    }

    describe("removeAmountQueryFromAccountUrl") {
        it("should remove amount query from account url") {
            val accountUrl = "test?amount=100&something=else"

            // Act & Assert
            sut.removeAmountQueryFromAccountUrl(accountUrl) shouldBe "test?something=else"
        }
    }

    describe("checkEmailAddress") {
        it("should not throw exception when email is matched") {
            val email = "test@test.com"
            val user = SampleEntity.user
            user.email = email

            // Act & Assert
            sut.checkEmailAddress(user, email)
        }

        it("should throw UserNotFoundException when email is not matched") {
            val email = "test@test.com"
            val wrongEmail = "wrong@wrong.com"
            val user = SampleEntity.user
            user.email = email

            // Act & Assert
            shouldThrow<UserNotFoundException> {
                sut.checkEmailAddress(user, wrongEmail)
            }
        }
    }

    describe("deleteUser") {
        it("should delete user") {
            val user = SampleEntity.user

            // Mock r
            every { repository.delete(user) } returns Unit

            // Act & Assert
            sut.deleteUser(user) shouldBe user
        }
    }

    describe("savePassword") {
        it("should save password") {
            val user = SampleEntity.user
            val password = "test"
            val encodedPassword = "encoded password"

            // Mock
            every { passwordEncoder.encode(password) } returns encodedPassword
            every { repository.save(user) } returns user

            // Act & Assert
            sut.savePassword(user, password).loginPassword shouldBe encodedPassword
        }
    }
})