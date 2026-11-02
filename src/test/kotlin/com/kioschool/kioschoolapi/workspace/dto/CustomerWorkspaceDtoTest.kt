package com.kioschool.kioschoolapi.workspace.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.kioschool.kioschoolapi.domain.workspace.dto.common.CustomerWorkspaceDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceDto
import com.kioschool.kioschoolapi.factory.SampleEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf

class CustomerWorkspaceDtoTest : DescribeSpec({
    val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    it("손님용 응답에는 주인 정보와 운영진 메모가 없다") {
        val dto = CustomerWorkspaceDto.of(WorkspaceDto.of(SampleEntity.workspace))
        val fields = objectMapper.readTree(objectMapper.writeValueAsString(dto)).fieldNames().asSequence().toList()

        fields shouldNotContainAnyOf listOf("owner", "memo")
        fields shouldContainAll listOf("id", "name", "products", "images", "description", "notice", "tableCount")
    }
})
