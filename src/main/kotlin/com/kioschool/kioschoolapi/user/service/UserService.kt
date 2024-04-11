package com.kioschool.kioschoolapi.user.service

import com.kioschool.kioschoolapi.bank.service.BankService
import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.discord.DiscordService
import com.kioschool.kioschoolapi.email.service.EmailService
import com.kioschool.kioschoolapi.security.JwtProvider
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.exception.*
import com.kioschool.kioschoolapi.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
    private val bankService: BankService,
    private val discordService: DiscordService
) {
    fun login(loginId: String, loginPassword: String): String {
        val user = getUser(loginId)

        if (!passwordEncoder.matches(
                loginPassword,
                user.loginPassword
            )
        ) throw LoginFailedException()

        return jwtProvider.createToken(user)
    }

    fun register(loginId: String, loginPassword: String, name: String, email: String): String {
        if (isDuplicateLoginId(loginId)) throw RegisterException("이미 사용하고 있는 아이디입니다.")
        if (!emailService.isEmailVerified(email)) throw RegisterException("이메일 인증이 안된 이메일입니다.")
        if (isDuplicateEmail(email)) throw RegisterException("이미 사용하고 있는 이메일입니다.")
        emailService.deleteRegisterCode(email)

        val user = userRepository.save(
            User(
                loginId = loginId,
                loginPassword = passwordEncoder.encode(loginPassword),
                name = name,
                email = email,
                role = UserRole.ADMIN,
                members = mutableListOf()
            )
        )

        discordService.sendUserRegister(user)
        return jwtProvider.createToken(user)
    }

    fun isDuplicateLoginId(loginId: String): Boolean {
        return userRepository.findByLoginId(loginId) != null
    }

    fun isDuplicateEmail(email: String): Boolean {
        return userRepository.findByEmail(email) != null
    }

    fun getUser(loginId: String): User {
        return userRepository.findByLoginId(loginId) ?: throw LoginFailedException()
    }

    fun createSuperUser(username: String, id: String): User {
        val superAdminUser = getUser(username)
        if (superAdminUser.role != UserRole.SUPER_ADMIN) throw NoPermissionException()

        val user = userRepository.findByLoginId(id) ?: throw UserNotFoundException()
        user.role = UserRole.ADMIN
        return userRepository.save(user)
    }

    fun registerAccountUrl(username: String, accountUrl: String): User {
        val user = getUser(username)
        user.accountUrl = accountUrl.replace(Regex("amount=\\d+&"), "")

        checkBankHolderNameMatched(accountUrl, user.name)

        return userRepository.save(user)
    }

    private fun checkBankHolderNameMatched(accountUrl: String, username: String) {
        val bankName = extractBankName(accountUrl)
        val accountNumber = extractAccountNumber(accountUrl)
        val bankHolderName = bankService.getBankAccountHolderName(bankName, accountNumber)

        if (bankHolderName != username) throw BankHolderNotMatchedException()
    }

    private fun extractBankName(accountUrl: String): String {
        return accountUrl.substringAfter("bank=").substringBefore("&")
    }

    private fun extractAccountNumber(accountUrl: String): String {
        return accountUrl.substringAfter("accountNo=").substringBefore("&")
    }

    fun sendResetPasswordEmail(loginId: String, email: String) {
        val user = userRepository.findByLoginId(loginId) ?: throw UserNotFoundException()
        if (user.email != email) throw UserNotFoundException()

        emailService.sendResetPasswordEmail(email)
    }

    fun deleteUser(username: String): User {
        val user = getUser(username)
        userRepository.delete(user)
        return user
    }

    fun resetPassword(code: String, password: String) {
        val email = emailService.getEmailByCode(code) ?: throw UserNotFoundException()
        val user = userRepository.findByEmail(email) ?: throw UserNotFoundException()
        user.loginPassword = passwordEncoder.encode(password)
        userRepository.save(user)
    }
}