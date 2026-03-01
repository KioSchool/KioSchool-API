package com.kioschool.kioschoolapi.domain.statistics.entity.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kioschool.kioschoolapi.domain.statistics.dto.SalesByHour
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class SalesByHourListConverter : AttributeConverter<List<SalesByHour>, String> {
    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<SalesByHour>?): String {
        return attribute?.let { mapper.writeValueAsString(it) } ?: "[]"
    }

    override fun convertToEntityAttribute(dbData: String?): List<SalesByHour> {
        return dbData?.let { mapper.readValue(it, object : TypeReference<List<SalesByHour>>() {}) } ?: emptyList()
    }
}
