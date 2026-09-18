package com.kioschool.kioschoolapi.domain.user.repository

import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AcquisitionSurveyRepository : JpaRepository<AcquisitionSurvey, Long> {
    fun findByUser(user: User): AcquisitionSurvey?

    @Query("SELECT u.email, s.channel FROM AcquisitionSurvey s JOIN s.user u")
    fun findAllEmailAndChannel(): List<Array<Any?>>

    fun countByContextIsNotNull(): Long

    @Query(
        value = "SELECT s FROM AcquisitionSurvey s JOIN FETCH s.user WHERE (:channel IS NULL OR s.channel = :channel)",
        countQuery = "SELECT COUNT(s) FROM AcquisitionSurvey s WHERE (:channel IS NULL OR s.channel = :channel)"
    )
    fun findAllWithUser(channel: AcquisitionChannel?, pageable: Pageable): Page<AcquisitionSurvey>

    @Query(
        value = "SELECT s FROM AcquisitionSurvey s JOIN FETCH s.user u " +
            "WHERE (:channel IS NULL OR s.channel = :channel) " +
            "AND SUBSTRING(u.email, LOCATE('@', u.email) + 1) IN :emailDomains",
        countQuery = "SELECT COUNT(s) FROM AcquisitionSurvey s JOIN s.user u " +
            "WHERE (:channel IS NULL OR s.channel = :channel) " +
            "AND SUBSTRING(u.email, LOCATE('@', u.email) + 1) IN :emailDomains"
    )
    fun findAllWithUserByEmailDomains(
        channel: AcquisitionChannel?,
        emailDomains: Collection<String>,
        pageable: Pageable
    ): Page<AcquisitionSurvey>

    @Query(
        value = "SELECT s FROM AcquisitionSurvey s JOIN FETCH s.user u " +
            "WHERE (:channel IS NULL OR s.channel = :channel) AND u.email IS NULL",
        countQuery = "SELECT COUNT(s) FROM AcquisitionSurvey s JOIN s.user u " +
            "WHERE (:channel IS NULL OR s.channel = :channel) AND u.email IS NULL"
    )
    fun findAllWithUserWithoutEmail(channel: AcquisitionChannel?, pageable: Pageable): Page<AcquisitionSurvey>
}
