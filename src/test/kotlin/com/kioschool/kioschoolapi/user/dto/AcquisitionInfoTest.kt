package com.kioschool.kioschoolapi.user.dto

import com.kioschool.kioschoolapi.domain.user.dto.common.AcquisitionInfo
import com.kioschool.kioschoolapi.global.common.enums.AcquisitionChannel
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AcquisitionInfoTest : DescribeSpec({
    describe("normalized") {
        it("should drop channelEtc when channel is not ETC") {
            val info = AcquisitionInfo(AcquisitionChannel.INSTAGRAM, "친구가 알려줌", null)

            val result = info.normalized()

            result.channel shouldBe AcquisitionChannel.INSTAGRAM
            result.channelEtc shouldBe null
        }

        it("should keep channelEtc when channel is ETC") {
            val info = AcquisitionInfo(AcquisitionChannel.ETC, "교수님 추천", null)

            val result = info.normalized()

            result.channelEtc shouldBe "교수님 추천"
        }

        it("should allow ETC without channelEtc") {
            val info = AcquisitionInfo(AcquisitionChannel.ETC, null, null)

            val result = info.normalized()

            result.channel shouldBe AcquisitionChannel.ETC
            result.channelEtc shouldBe null
        }

        it("should convert blank channelEtc to null") {
            val info = AcquisitionInfo(AcquisitionChannel.ETC, "   ", null)

            val result = info.normalized()

            result.channelEtc shouldBe null
        }

        it("should keep context regardless of channel") {
            val info = AcquisitionInfo(null, null, "source=instagram")

            val result = info.normalized()

            result.channel shouldBe null
            result.context shouldBe "source=instagram"
        }
    }
})
