package com.kioschool.kioschoolapi.global.logging.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked

/**
 * 서버 로그(GCP Logging, 국외)에 개인정보를 남기지 않도록 가린다. 처리방침은 서버 로그 항목을 "접속 IP, 요청 경로"로만 적고 있다.
 *
 * 디버깅에 쓰는 ID·아이디·상품명·금액은 남긴다. 이메일은 가입 도메인 분석에 쓰이므로 도메인을 남긴다.
 */
object LogMasking {
    const val MASK = "****"

    private val MASKED_PROPERTIES = setOf(
        "customerName",
        "accountNumber",
        "accountHolder",
        "accountUrl",
        "tossAccountUrl",
        "ownerName",
        // 문의·답변 본문과 가입 경로 설문의 자유 입력
        "subject",
        "content",
        "context",
        "channelEtc",
    )

    /** 로그 직렬화 전용 복사본. 원래 [base]와 HTTP 응답은 바뀌지 않는다. */
    fun loggingMapper(base: ObjectMapper): ObjectMapper =
        base.copy().registerModule(SimpleModule("LogMasking").setSerializerModifier(MaskingModifier))

    /** 이름이 있는 값 하나를 가린다. 맨 값 인자(`@RequestParam email`)나 검증 오류의 필드 경로(`orders[0].customerName`)에 쓴다. */
    fun maskValue(name: String, value: String?): String? {
        value ?: return null
        val property = name.substringAfterLast('.')
        return when {
            isEmailProperty(property) -> maskEmail(value)
            property in MASKED_PROPERTIES -> MASK
            else -> value
        }
    }

    fun maskEmail(email: String): String {
        val at = email.lastIndexOf('@')
        return if (at < 0) MASK else MASK + email.substring(at)
    }

    private fun isEmailProperty(name: String) = name == "email" || name.endsWith("Email")

    private object MaskingModifier : BeanSerializerModifier() {
        override fun changeProperties(
            config: SerializationConfig,
            beanDesc: BeanDescription,
            beanProperties: MutableList<BeanPropertyWriter>,
        ): MutableList<BeanPropertyWriter> {
            beanProperties.forEach { writer ->
                // @Masked(@JsonSerialize)처럼 이미 직렬화기가 정해진 속성은 그대로 둔다
                if (writer.hasSerializer()) return@forEach
                serializerFor(writer)?.let { writer.assignSerializer(it) }
            }
            return beanProperties
        }

        private fun serializerFor(writer: BeanPropertyWriter): StdSerializer<Any>? {
            val isString = CharSequence::class.java.isAssignableFrom(writer.type.rawClass)
            // 이름 규칙은 문자열에만 적용한다. Spring Page의 content처럼 같은 이름의 목록·객체는 안에서 다시 가린다
            return when {
                writer.getAnnotation(LogMasked::class.java) != null -> FullMaskSerializer
                !isString -> null
                writer.name in MASKED_PROPERTIES -> FullMaskSerializer
                isEmailProperty(writer.name) -> EmailMaskSerializer
                else -> null
            }
        }
    }

    private object FullMaskSerializer : StdSerializer<Any>(Any::class.java) {
        override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) = gen.writeString(MASK)
    }

    private object EmailMaskSerializer : StdSerializer<Any>(Any::class.java) {
        override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) =
            gen.writeString(maskEmail(value.toString()))
    }
}
