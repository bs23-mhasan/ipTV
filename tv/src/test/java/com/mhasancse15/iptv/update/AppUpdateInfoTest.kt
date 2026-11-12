package com.mhasancse15.iptv.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateInfoTest {
    @Test
    fun installedVersionAtOrAboveLatestIsUpToDate() {
        val info = appInfo(latestVersionCode = 4)

        assertEquals(
            UpdateDecision.UpToDate(4),
            evaluateUpdate(installedVersionCode = 4, info)
        )
        assertEquals(
            UpdateDecision.UpToDate(5),
            evaluateUpdate(installedVersionCode = 5, info)
        )
    }

    @Test
    fun versionBelowMinimumGetsMandatoryUpdate() {
        val decision = evaluateUpdate(installedVersionCode = 2, appInfo(minimumVersionCode = 3))

        assertTrue((decision as UpdateDecision.Available).mandatory)
    }

    @Test
    fun forceUpdateMakesNewerVersionMandatory() {
        val decision = evaluateUpdate(installedVersionCode = 2, appInfo(forceUpdate = true))

        assertTrue((decision as UpdateDecision.Available).mandatory)
    }

    @Test
    fun newerVersionCanBeDeferredWhenNotMandatory() {
        val decision = evaluateUpdate(installedVersionCode = 2, appInfo())

        assertFalse((decision as UpdateDecision.Available).mandatory)
    }

    private fun appInfo(
        latestVersionCode: Long = 4,
        minimumVersionCode: Long = 1,
        forceUpdate: Boolean = false
    ) = AppUpdateInfo(
        appName = "BDIX TV",
        latestVersion = "1.2.0",
        latestVersionCode = latestVersionCode,
        minimumSupportedVersionCode = minimumVersionCode,
        downloadUrl = "https://example.com/app.apk",
        releaseNotes = listOf("Improvements"),
        forceUpdate = forceUpdate
    )
}
