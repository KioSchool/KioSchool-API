package com.kioschool.kioschoolapi.domain.inquiry.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "inquiry_image")
class InquiryImage(
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    val inquiry: Inquiry,
    // S3 객체 키. 외부 응답에 절대 싣지 않는다. URL은 조회 시점에 계산한다.
    val storageKey: String,
    val originalFileName: String,
    val contentType: String,
    val size: Long,
) : BaseEntity()
