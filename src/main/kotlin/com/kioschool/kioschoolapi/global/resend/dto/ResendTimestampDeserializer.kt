package com.kioschool.kioschoolapi.global.resend.dto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

// 시각 형식 하나 때문에 대화 전체를 못 읽으면 메일을 잃는다. 읽을 수 없으면 지금 시각으로 저장한다.
class ResendTimestampDeserializer : StdDeserializer<OffsetDateTime>(OffsetDateTime::class.java) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): OffsetDateTime = parse(parser.text)

    override fun getNullValue(context: DeserializationContext): OffsetDateTime = OffsetDateTime.now()

    private fun parse(value: String): OffsetDateTime {
        val iso = value.trim().replace(' ', 'T').replace(SHORT_OFFSET, "$1:00")
        return try {
            OffsetDateTime.parse(iso)
        } catch (e: DateTimeParseException) {
            OffsetDateTime.now()
        }
    }

    companion object {
        private val SHORT_OFFSET = Regex("([+-]\\d{2})$")
    }
}
