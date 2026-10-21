package com.kioschool.kioschoolapi.global.cache.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceImageDto
import com.kioschool.kioschoolapi.factory.SampleEntity.newProduct
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

    // 배포 전 캐시 모양은 손으로 쓰지 않고, 지금 DTO를 직렬화한 뒤 새로 생긴 키를 지워 만든다.
    describe("배포 전 캐시 호환") {
        it("discountPrice가 없던 ProductDto 캐시를 읽는다") {
            val legacy = withoutKeys(serializer.serialize(ProductDto.of(newProduct(1))), "discountPrice")

            (serializer.deserialize(legacy) as ProductDto).discountPrice shouldBe null
        }
    }
})

private fun withoutKeys(bytes: ByteArray?, vararg keys: String): ByteArray {
    val mapper = ObjectMapper()
    val tree = mapper.readTree(bytes)
    (tree[1] as ObjectNode).remove(keys.toList())
    return mapper.writeValueAsBytes(tree)
}
