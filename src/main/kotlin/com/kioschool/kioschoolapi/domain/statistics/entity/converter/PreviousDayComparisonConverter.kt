package com.kioschool.kioschoolapi.domain.statistics.entity.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kioschool.kioschoolapi.domain.statistics.dto.PreviousDayComparison
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class PreviousDayComparisonConverter : AttributeConverter<PreviousDayComparison, String> {
    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: PreviousDayComparison?): String? {
        return attribute?.let { mapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): PreviousDayComparison? {
        return dbData?.let { mapper.readValue<PreviousDayComparison>(it) }
    }
}
