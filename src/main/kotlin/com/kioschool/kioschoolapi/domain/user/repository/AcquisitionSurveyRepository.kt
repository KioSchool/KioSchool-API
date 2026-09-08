package com.kioschool.kioschoolapi.domain.user.repository

import com.kioschool.kioschoolapi.domain.user.entity.AcquisitionSurvey
import com.kioschool.kioschoolapi.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AcquisitionSurveyRepository : JpaRepository<AcquisitionSurvey, Long> {
    fun findByUser(user: User): AcquisitionSurvey?
}
