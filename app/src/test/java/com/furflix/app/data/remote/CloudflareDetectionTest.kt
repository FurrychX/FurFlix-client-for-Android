package com.furflix.app.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareDetectionTest {

    @Test
    fun `403 with Cf-Mitigated challenge and empty body is detected`() {
        assertTrue(
            CloudflareDetector.isChallenge(
                code = 403,
                cfMitigated = "challenge",
                cfRay = "1234567890abcdef-WAW",
                html = ""
            )
        )
    }

    @Test
    fun `403 with CF-RAY header and empty body is detected`() {
        assertTrue(
            CloudflareDetector.isChallenge(
                code = 403,
                cfMitigated = null,
                cfRay = "1234567890abcdef-WAW",
                html = ""
            )
        )
    }

    @Test
    fun `200 with Cloudflare JS challenge page is detected`() {
        val html = """
            <html><head><title>Just a moment...</title></head>
            <body><div id="challenge-platform"></div></body></html>
        """.trimIndent()
        assertTrue(
            CloudflareDetector.isChallenge(
                code = 200,
                cfMitigated = null,
                cfRay = "12345",
                html = html
            )
        )
    }

    @Test
    fun `200 with normal FurAffinity browse page is not flagged`() {
        val html = """
            <html><head><title>Browse -- Fur Affinity [dot] net</title></head>
            <body><section id="gallery-search-results"><figure id="sid-12345"></figure></section></body></html>
        """.trimIndent()
        assertFalse(
            CloudflareDetector.isChallenge(
                code = 200,
                cfMitigated = null,
                cfRay = "12345",
                html = html
            )
        )
    }
}
