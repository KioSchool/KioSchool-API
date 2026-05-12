package com.kioschool.kioschoolapi.domain.insight.entity.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kioschool.kioschoolapi.domain.insight.dto.MetricSummary
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class MetricSummaryListConverter : AttributeConverter<List<MetricSummary>, String> {
    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<MetricSummary>?): String? =
        attribute?.let { mapper.writeValueAsString(it) }

    override fun convertToEntityAttribute(dbData: String?): List<MetricSummary> =
        if (dbData.isNullOrBlank()) emptyList()
        else mapper.readValue(dbData)
}
