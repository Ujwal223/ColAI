package com.ujwal.colai.core.engine

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeckoRuntimeManagerTest {

    @Test
    fun testGeckoRuntimeManagerClassExists() {
        assertNotNull(GeckoRuntimeManager::class.java)
    }

    @Test
    fun testUninitializedGetRuntimeThrowsOrReturns() {
        if (!GeckoRuntimeManager.isInitialized) {
            try {
                GeckoRuntimeManager.getRuntime()
                org.junit.Assert.fail("Expected IllegalStateException when uninitialized")
            } catch (e: IllegalStateException) {
                assertTrue(e.message!!.contains("GeckoRuntime has not been initialized"))
            }
        }
    }
}
