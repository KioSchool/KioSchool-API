package com.kioschool.kioschoolapi.global.logging.annotation

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.kioschool.kioschoolapi.global.logging.util.MaskingSerializer

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = MaskingSerializer::class)
annotation class Masked