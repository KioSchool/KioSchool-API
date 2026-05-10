package com.kioschool.kioschoolapi.domain.insight.entity.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kioschool.kioschoolapi.domain.insight.entity.CardPayload
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class CardPayloadConverter : AttributeConverter<CardPayload, String> {
    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: CardPayload?): String? {
        return attribute?.let { mapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): CardPayload? {
        return dbData?.let { mapper.readValue<CardPayload>(it) }
    }
}
