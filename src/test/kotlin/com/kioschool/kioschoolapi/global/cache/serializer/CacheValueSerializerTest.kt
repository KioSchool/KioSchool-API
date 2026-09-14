package com.kioschool.kioschoolapi.global.cache.serializer

import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceImageDto
import com.kioschool.kioschoolapi.global.configuration.CacheConfiguration
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

class CacheValueSerializerTest : DescribeSpec({
    val serializer = CacheConfiguration.cacheValueSerializer()

    describe("FocalPointDto") {
        it("should survive a cache round trip") {
            val image = WorkspaceImageDto(1L, "https://example.com/a.png", FocalPointDto(30, 12), null, null)

            serializer.deserialize(serializer.serialize(image)) shouldBe image
        }

        it("should not write the range check as a field") {
            String(serializer.serialize(FocalPointDto(30, 12))!!) shouldNotContain "InRange"
        }

        it("should read entries written before the range check was excluded") {
            val legacy = """["${FocalPointDto::class.java.name}",{"x":30,"y":12,"isInRange":true}]"""

            serializer.deserialize(legacy.toByteArray()) shouldBe FocalPointDto(30, 12)
        }
    }
})
