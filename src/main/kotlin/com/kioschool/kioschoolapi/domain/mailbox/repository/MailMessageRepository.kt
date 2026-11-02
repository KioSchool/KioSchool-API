package com.kioschool.kioschoolapi.domain.mailbox.repository

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailMessage
import com.kioschool.kioschoolapi.domain.mailbox.entity.MailThread
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface MailMessageRepository : JpaRepository<MailMessage, Long> {
    fun findByResendEmailId(resendEmailId: String): MailMessage?

    fun findAllByThreadOrderBySentAtAscIdAsc(thread: MailThread): List<MailMessage>

    // 보낸 기록만 남고 스레드와 연결되지 못한 메일. 대화 삭제로 지워지지 않으므로 따로 지운다.
    @Modifying
    @Transactional
    @Query("delete from MailMessage m where m.thread is null and m.createdAt < :before")
    fun deleteOrphansCreatedBefore(@Param("before") before: LocalDateTime): Int
}
