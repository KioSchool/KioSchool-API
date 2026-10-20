package com.kioschool.kioschoolapi.domain.email.entity

import com.kioschool.kioschoolapi.domain.email.enum.EmailKind
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "email_code", schema = "PUBLIC")
class EmailCode(
    @Column(unique = true)
    val email: String,
    var code: String,
    var isVerified: Boolean = false,
    var kind: EmailKind,
    // 가입 인증 코드에만 쓴다. 비밀번호 재설정 코드는 추측할 수 없는 UUID라 두지 않는다.
    var attemptCount: Int = 0,
    var expiresAt: LocalDateTime? = null
) : BaseEntity()
