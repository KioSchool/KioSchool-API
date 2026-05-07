package com.kioschool.kioschoolapi.og.controller

import com.kioschool.kioschoolapi.global.og.controller.OgController
import com.kioschool.kioschoolapi.global.og.facade.OgFacade
import com.kioschool.kioschoolapi.global.og.facade.ShareLinkAction
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.URI

class OgControllerTest : DescribeSpec({
    val ogFacade = mockk<OgFacade>()
    val sut = OgController(ogFacade)

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
        // controller는 분기 로직을 갖지 않고 facade가 반환한 ShareLinkAction을
        // HTTP 응답으로 래핑만 한다. 분기 자체의 검증은 OgFacadeTest에서.
        it("wraps RenderOgHtml action as 200 + text/html;charset=UTF-8 + 10min public cache") {
            every {
                ogFacade.resolveShareLink(42L, null, null, "KAKAOTALK")
            } returns ShareLinkAction.RenderOgHtml("<html>og</html>")

            val response = sut.shareLink(42L, null, null, "KAKAOTALK")

            assert(response.statusCode.value() == 200)
            assert(response.body == "<html>og</html>")
            val contentType = response.headers.contentType?.toString() ?: ""
            assert(contentType.startsWith("text/html"))
            assert(contentType.contains("UTF-8", ignoreCase = true))
            val cacheControl = response.headers.cacheControl ?: ""
            assert(cacheControl.contains("max-age=600"))
            assert(cacheControl.contains("public"))
        }

        it("wraps RedirectToOrder action as 302 + Location header") {
            val target = URI.create("https://kio-school.com/order?workspaceId=42")
            every {
                ogFacade.resolveShareLink(42L, null, null, "Chrome")
            } returns ShareLinkAction.RedirectToOrder(target)

            val response = sut.shareLink(42L, null, null, "Chrome")

            assert(response.statusCode.value() == 302)
            assert(response.headers.location == target)
            assert(response.body == null)
        }

        it("forwards all params to facade verbatim") {
            every {
                ogFacade.resolveShareLink(7L, 3, "abc123", "Chrome")
            } returns ShareLinkAction.RedirectToOrder(URI.create("https://example/dummy"))

            sut.shareLink(7L, 3, "abc123", "Chrome")

            verify(exactly = 1) { ogFacade.resolveShareLink(7L, 3, "abc123", "Chrome") }
        }
    }
})
