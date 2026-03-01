package com.kioschool.kioschoolapi.domain.statistics.entity.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kioschool.kioschoolapi.domain.statistics.dto.PopularProducts
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class PopularProductsConverter : AttributeConverter<PopularProducts, String> {
    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: PopularProducts?): String? {
        return attribute?.let { mapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): PopularProducts? {
        return dbData?.let { mapper.readValue<PopularProducts>(it) }
    }
}
