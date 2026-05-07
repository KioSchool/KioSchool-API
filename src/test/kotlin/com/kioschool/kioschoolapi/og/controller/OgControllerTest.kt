package com.kioschool.kioschoolapi.og.controller

import com.kioschool.kioschoolapi.global.og.controller.OgController
import com.kioschool.kioschoolapi.global.og.facade.OgFacade
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class OgControllerTest : DescribeSpec({
    val ogFacade = mockk<OgFacade>()
    val sut = OgController(ogFacade, baseUrl = "https://kio-school.com")

    beforeEach { clearMocks(ogFacade) }

    describe("ogOrder") {
        it("delegates to OgFacade.renderOrderHtml and wraps the body with cache headers") {
            every { ogFacade.renderOrderHtml(42L) } returns "<html>body</html>"

            val response = sut.ogOrder(42L)

            assert(response.statusCode.value() == 200)
            assert(response.body == "<html>body</html>")
            val cacheControl = response.headers.cacheControl ?: ""
            assert(cacheControl.contains("max-age=600"))
            assert(cacheControl.contains("public"))
            verify(exactly = 1) { ogFacade.renderOrderHtml(42L) }
        }

        it("forwards null workspaceId untouched") {
            every { ogFacade.renderOrderHtml(null) } returns "<html>fallback</html>"

            val response = sut.ogOrder(null)

            assert(response.body == "<html>fallback</html>")
            verify(exactly = 1) { ogFacade.renderOrderHtml(null) }
        }
    }

    describe("shareLink") {
        val botUas = listOf(
            "Mozilla/5.0 (compatible; KAKAOTALK 9.0.0)",
            "facebookexternalhit/1.1",
            "Slackbot-LinkExpanding 1.0",
            "Mozilla/5.0 (compatible; Googlebot/2.1)",
            "Twitterbot/1.0",
            "Mozilla/5.0 (compatible; bingbot/2.0)",
        )

        botUas.forEach { ua ->
            it("returns og HTML for bot UA: ${ua.take(40)}") {
                every { ogFacade.renderOrderHtml(42L) } returns "<html>og</html>"

                val response = sut.shareLink(42L, tableNumber = null, tableHash = null, userAgent = ua)

                assert(response.statusCode.value() == 200)
                assert(response.body == "<html>og</html>")
                val contentType = response.headers.contentType?.toString() ?: ""
                assert(contentType.startsWith("text/html"))
                assert(contentType.contains("UTF-8", ignoreCase = true))
                val cacheControl = response.headers.cacheControl ?: ""
                assert(cacheControl.contains("max-age=600"))
                assert(cacheControl.contains("public"))
                verify(exactly = 1) { ogFacade.renderOrderHtml(42L) }
            }
        }

        it("og card is workspace-level and ignores table params even when present") {
            every { ogFacade.renderOrderHtml(42L) } returns "<html>og</html>"

            val response = sut.shareLink(
                workspaceId = 42L,
                tableNumber = 3,
                tableHash = "abc123",
                userAgent = "Mozilla/5.0 (compatible; KAKAOTALK 9.0.0)",
            )

            assert(response.statusCode.value() == 200)
            assert(response.body == "<html>og</html>")
            // og 카드는 워크스페이스 단위라 테이블 무관 — renderOrderHtml(workspaceId)만 호출
            verify(exactly = 1) { ogFacade.renderOrderHtml(42L) }
        }

        val humanUas = listOf(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/120.0",
        )

        humanUas.forEach { ua ->
            it("returns 302 redirect for human UA: ${ua.take(40)}") {
                val response = sut.shareLink(42L, tableNumber = null, tableHash = null, userAgent = ua)

                assert(response.statusCode.value() == 302)
                val location = response.headers.location?.toString() ?: ""
                assert(location == "https://kio-school.com/order?workspaceId=42") {
                    "Expected redirect to canonical order URL, got: $location"
                }
                assert(response.body == null)
                verify(exactly = 0) { ogFacade.renderOrderHtml(any()) }
            }
        }

        it("treats missing UA header as a human (302 redirect)") {
            val response = sut.shareLink(42L, tableNumber = null, tableHash = null, userAgent = null)

            assert(response.statusCode.value() == 302)
            val location = response.headers.location?.toString() ?: ""
            assert(location == "https://kio-school.com/order?workspaceId=42")
            verify(exactly = 0) { ogFacade.renderOrderHtml(any()) }
        }

        it("treats blank UA header as a human (302 redirect)") {
            val response = sut.shareLink(42L, tableNumber = null, tableHash = null, userAgent = "")

            assert(response.statusCode.value() == 302)
            verify(exactly = 0) { ogFacade.renderOrderHtml(any()) }
        }

        it("uses configured baseUrl for the redirect target") {
            val devSut = OgController(ogFacade, baseUrl = "https://dev.kio-school.com")

            val response = devSut.shareLink(7L, tableNumber = null, tableHash = null, userAgent = "Chrome")

            assert(response.headers.location?.toString() == "https://dev.kio-school.com/order?workspaceId=7")
        }

        it("preserves tableNumber and tableHash in the redirect target so a friend can join the same table session") {
            val response = sut.shareLink(
                workspaceId = 42L,
                tableNumber = 3,
                tableHash = "abc123",
                userAgent = "Chrome",
            )

            assert(response.statusCode.value() == 302)
            val location = response.headers.location?.toString() ?: ""
            assert(location == "https://kio-school.com/order?workspaceId=42&tableNumber=3&tableHash=abc123") {
                "Expected redirect to include both table params, got: $location"
            }
        }

        it("preserves only tableNumber when tableHash is absent") {
            val response = sut.shareLink(
                workspaceId = 42L,
                tableNumber = 3,
                tableHash = null,
                userAgent = "Chrome",
            )

            val location = response.headers.location?.toString() ?: ""
            assert(location == "https://kio-school.com/order?workspaceId=42&tableNumber=3")
        }

        it("preserves only tableHash when tableNumber is absent") {
            val response = sut.shareLink(
                workspaceId = 42L,
                tableNumber = null,
                tableHash = "abc123",
                userAgent = "Chrome",
            )

            val location = response.headers.location?.toString() ?: ""
            assert(location == "https://kio-school.com/order?workspaceId=42&tableHash=abc123")
        }

        it("URL-encodes tableHash to handle special characters safely") {
            val response = sut.shareLink(
                workspaceId = 42L,
                tableNumber = null,
                // 사실 tableHash는 보통 UUID 문자라 이런 특수문자가 안 들어오지만, 안전망 검증.
                tableHash = "hash with spaces & symbols",
                userAgent = "Chrome",
            )

            val location = response.headers.location?.toString() ?: ""
            // UriComponentsBuilder가 encoding 처리
            assert(!location.contains("hash with spaces")) { "Expected encoded output, got: $location" }
            assert(location.contains("workspaceId=42"))
            assert(location.contains("tableHash="))
        }
    }
})
