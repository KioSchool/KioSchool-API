package com.kioschool.kioschoolapi.domain.inquiry.repository

import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface InquiryRepository : JpaRepository<Inquiry, Long> {
    fun findAllByStatus(status: InquiryStatus, pageable: Pageable): Page<Inquiry>

    /**
     * 답변·종결 처리용. 같은 문의에 대한 동시 요청 중 하나만 진행시키기 위해 행을 잠근다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inquiry i where i.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Inquiry?

    fun findAllByPurgeAtBefore(purgeAt: LocalDateTime, pageable: Pageable): List<Inquiry>
}
