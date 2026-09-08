package com.kioschool.kioschoolapi.domain.user.entity

import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "acquisition_survey")
class AcquisitionSurvey(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    var user: User,
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    var channel: AcquisitionChannel? = null,
    @Column(length = 100)
    var channelEtc: String? = null,
    @Column(length = 500)
    var context: String? = null,
) : BaseEntity()
