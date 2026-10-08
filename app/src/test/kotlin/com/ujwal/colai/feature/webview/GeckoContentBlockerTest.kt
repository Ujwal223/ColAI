package com.ujwal.colai.feature.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.ContentBlocking

class GeckoContentBlockerTest {

    @Test
    fun testInitialShieldState() {
        val blocker = GeckoContentBlocker(initialShieldEnabled = true)
        val stats = blocker.shieldStats.value

        assertTrue(stats.isShieldEnabled)
        assertEquals(0, stats.totalBlocked)
        assertEquals(0, stats.adsBlocked)
        assertEquals(0, stats.analyticsBlocked)
        assertNull(stats.lastBlockedUri)
    }

    @Test
    fun testBlockEventIncrementsStats() {
        val blocker = GeckoContentBlocker()
        assertNotNull(blocker)

        val css = GeckoContentBlocker.getAntiAppBannerCss()
        assertNotNull(css)
        assertTrue(css.contains("display: none !important"))
        assertTrue(css.contains("play.google.com/store/apps"))
        assertTrue(css.contains(".mobile-app-promo"))
    }

    @Test
    fun testShieldToggleAndReset() {
        val blocker = GeckoContentBlocker()
        blocker.setShieldEnabled(false)
        assertFalse(blocker.shieldStats.value.isShieldEnabled)

        blocker.setShieldEnabled(true)
        assertTrue(blocker.shieldStats.value.isShieldEnabled)

        blocker.resetStats()
        assertEquals(0, blocker.shieldStats.value.totalBlocked)
    }

    @Test
    fun testAntiAppBannerSelectors() {
        val selectors = GeckoContentBlocker.ANTI_APP_BANNER_SELECTORS
        assertTrue(selectors.isNotEmpty())
        assertTrue(selectors.any { it.contains("play.google.com/store/apps") })
        assertTrue(selectors.any { it.contains("claude.ai/download") })
        assertTrue(selectors.any { it.contains("mobile-app-promo") })
    }
}
