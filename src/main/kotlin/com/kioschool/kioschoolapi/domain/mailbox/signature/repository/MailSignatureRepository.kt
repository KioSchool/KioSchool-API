package com.kioschool.kioschoolapi.domain.mailbox.signature.repository

import com.kioschool.kioschoolapi.domain.mailbox.signature.entity.MailSignature
import com.kioschool.kioschoolapi.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface MailSignatureRepository : JpaRepository<MailSignature, Long> {
    fun findByUser(user: User): MailSignature?

    /** 팀 공용은 DB 제약으로 한 줄뿐이다. */
    fun findFirstByUserIsNull(): MailSignature?

    fun deleteByUser(user: User)
}
