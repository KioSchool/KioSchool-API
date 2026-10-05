package com.kioschool.kioschoolapi.domain.user.repository

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.user.entity.UserSession
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserSessionRepository : JpaRepository<UserSession, Long> {
    /**
     * 갱신용. 여러 탭이 같은 토큰으로 동시에 갱신하면 하나씩 처리해야 교체와 유예가 갈린다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s where s.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): UserSession?

    @Modifying
    @Query("delete from UserSession s where s.user = :user")
    fun deleteAllByUser(@Param("user") user: User): Int

    @Modifying
    @Query("delete from UserSession s where s.expiresAt <= :now")
    fun deleteAllExpired(@Param("now") now: LocalDateTime): Int
}
