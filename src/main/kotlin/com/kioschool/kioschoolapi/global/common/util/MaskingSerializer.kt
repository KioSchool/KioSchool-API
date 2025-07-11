package com.kioschool.kioschoolapi.global.common.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class MaskingSerializer : JsonSerializer<Any>() {
    override fun serialize(value: Any?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value != null) {
            gen.writeString("****")
        } else {
            gen.writeNull()
        }
    }
}