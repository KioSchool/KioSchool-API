package com.kioschool.kioschoolapi.domain.user.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.exception.LoginFailedException
import com.kioschool.kioschoolapi.domain.user.exception.NoPermissionException
import com.kioschool.kioschoolapi.domain.user.exception.RegisterException
import com.kioschool.kioschoolapi.domain.user.exception.UserNotFoundException
import com.kioschool.kioschoolapi.domain.user.repository.UserRepository
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
) {
    fun checkPassword(user: User, loginPassword: String) {
        if (!passwordEncoder.matches(
                loginPassword,
                user.loginPassword
            )
        ) throw LoginFailedException()
    }

    fun saveUser(user: User): User {
        return userRepository.save(user)
    }

    fun saveUser(loginId: String, loginPassword: String, name: String, email: String): User {
        return userRepository.save(
            User(
                loginId = loginId,
                loginPassword = passwordEncoder.encode(loginPassword),
                name = name,
                email = email,
                role = UserRole.ADMIN,
                members = mutableListOf()
            )
        )
    }

    fun validateLoginId(loginId: String) {
        if (isDuplicateLoginId(loginId)) throw RegisterException()
    }

    fun validateEmail(email: String) {
        checkIsEmailVerified(email)
        checkIsEmailDuplicate(email)
    }

    fun isDuplicateLoginId(loginId: String): Boolean {
        return userRepository.findByLoginId(loginId) != null
    }

    private fun checkIsEmailVerified(email: String) {
        if (!emailService.isRegisterEmailVerified(email)) throw RegisterException()
    }

    private fun checkIsEmailDuplicate(email: String) {
        if (isDuplicateEmail(email)) throw RegisterException()
    }

    private fun isDuplicateEmail(email: String): Boolean {
        return userRepository.findByEmail(email) != null
    }

    fun getUser(loginId: String): User {
        return userRepository.findByLoginId(loginId) ?: throw UserNotFoundException()
    }

    fun getUserByEmail(email: String): User {
        return userRepository.findByEmail(email) ?: throw UserNotFoundException()
    }

    fun getAllUsers(name: String?, page: Int, size: Int): Page<User> {
        if (!name.isNullOrBlank()) {
            return userRepository.findByNameContains(
                name,
                PageRequest.of(page, size)
            )
        }

        return userRepository.findAll(PageRequest.of(page, size))
    }

    fun isSuperAdminUser(username: String): Boolean {
        return getUser(username).role == UserRole.SUPER_ADMIN
    }

    fun checkHasSuperAdminPermission(user: User) {
        if (user.role != UserRole.SUPER_ADMIN) throw NoPermissionException()
    }

    fun removeAmountQueryFromAccountUrl(accountUrl: String): String {
        return accountUrl.replace(Regex("amount=\\d+&"), "")
    }

    fun checkEmailAddress(user: User, email: String) {
        if (user.email != email) throw UserNotFoundException()
    }

    fun deleteUser(user: User): User {
        userRepository.delete(user)
        return user
    }

    fun savePassword(user: User, password: String): User {
        user.loginPassword = passwordEncoder.encode(password)
        return userRepository.save(user)
    }
}