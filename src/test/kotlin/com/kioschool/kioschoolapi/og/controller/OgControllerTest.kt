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
})
