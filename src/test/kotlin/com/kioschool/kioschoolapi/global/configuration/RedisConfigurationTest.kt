package com.kioschool.kioschoolapi.global.configuration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.lettuce.core.resource.ClientResources
import io.mockk.mockk
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory

class RedisConfigurationTest : DescribeSpec({
    describe("redisConnectionFactory") {
        // Boot가 만든 ClientResources에 Redis 명령 지연 메트릭(lettuce.command.*) 기록기가 붙어 있다.
        // 팩토리가 이걸 안 쓰고 자체 리소스를 만들면 Redis 메트릭이 하나도 안 나온다
        it("uses the Spring-managed ClientResources so command latency metrics are recorded") {
            val clientResources = mockk<ClientResources>()
            val sut = RedisConfiguration("localhost", 6379, "pw", false, clientResources)

            val factory = sut.redisConnectionFactory() as LettuceConnectionFactory

            factory.clientConfiguration.clientResources.get() shouldBeSameInstanceAs clientResources
        }
    }
})
