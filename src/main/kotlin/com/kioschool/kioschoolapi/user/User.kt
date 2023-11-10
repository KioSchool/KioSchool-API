package com.kioschool.kioschoolapi.user

import com.kioschool.kioschoolapi.common.entity.BaseEntity
import com.kioschool.kioschoolapi.common.enums.UserRole
import jakarta.persistence.Entity

@Entity
class User(
    var loginId: String,
    var loginPassword: String,
    var name: String,
    var email: String,
    var role: UserRole
) : BaseEntity()