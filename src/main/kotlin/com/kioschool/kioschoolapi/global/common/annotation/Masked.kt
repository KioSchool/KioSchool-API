package com.kioschool.kioschoolapi.global.common.annotation

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.kioschool.kioschoolapi.global.common.util.MaskingSerializer

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD

@Target(FIELD)
@Retention(RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = MaskingSerializer::class)
annotation class Masked