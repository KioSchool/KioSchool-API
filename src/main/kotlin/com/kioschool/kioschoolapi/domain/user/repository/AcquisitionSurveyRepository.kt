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

    @Query("SELECT s.channel, COUNT(s) FROM AcquisitionSurvey s GROUP BY s.channel")
    fun countGroupByChannel(): List<Array<Any?>>

    fun countByContextIsNotNull(): Long

    @Query(
        value = "SELECT s FROM AcquisitionSurvey s JOIN FETCH s.user WHERE (:channel IS NULL OR s.channel = :channel)",
        countQuery = "SELECT COUNT(s) FROM AcquisitionSurvey s WHERE (:channel IS NULL OR s.channel = :channel)"
    )
    fun findAllWithUser(channel: AcquisitionChannel?, pageable: Pageable): Page<AcquisitionSurvey>
}
