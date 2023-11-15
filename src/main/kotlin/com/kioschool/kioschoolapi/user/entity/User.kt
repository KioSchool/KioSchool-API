package com.kioschool.kioschoolapi.user.entity

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.common.enums.UserRole
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "user", schema = "PUBLIC")
class User(
    var loginId: String,
    var loginPassword: String,
    var name: String,
    var email: String,
    var role: UserRole
) : BaseEntity()