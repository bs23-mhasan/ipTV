package com.mhasancse15.iptv.update

import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URI

data class AppUpdateInfo(
    val appName: String,
    val latestVersion: String,
    val latestVersionCode: Long,
    val minimumSupportedVersionCode: Long,
    val downloadUrl: String,
    val releaseNotes: List<String>,
    val forceUpdate: Boolean
) {
    companion object {
        fun parse(json: String): AppUpdateInfo {
            try {
                val objectValue = JSONObject(json)
                val notes = objectValue.getJSONArray("release_notes")
                val info = AppUpdateInfo(
                    appName = objectValue.getString("app_name"),
                    latestVersion = objectValue.getString("latest_version"),
                    latestVersionCode = objectValue.getLong("latest_version_code"),
                    minimumSupportedVersionCode =
                        objectValue.getLong("minimum_supported_version_code"),
                    downloadUrl = objectValue.getString("download_url"),
                    releaseNotes = (0 until notes.length()).map { index ->
                        notes.getString(index)
                    },
                    forceUpdate = objectValue.optBoolean("force_update", false)
                )
                info.validate()
                return info
            } catch (error: JSONException) {
                throw IOException("Update manifest is missing valid required fields", error)
            } catch (error: IllegalArgumentException) {
                throw IOException("Update manifest contains invalid values: ${error.message}", error)
            }
        }
    }

    private fun validate() {
        require(appName.isNotBlank()) { "app_name must not be blank" }
        require(latestVersion.isNotBlank()) { "latest_version must not be blank" }
        require(latestVersionCode > 0) { "latest_version_code must be positive" }
        require(minimumSupportedVersionCode >= 0) {
            "minimum_supported_version_code must not be negative"
        }
        require(minimumSupportedVersionCode <= latestVersionCode) {
            "minimum_supported_version_code cannot exceed latest_version_code"
        }
        require(releaseNotes.none(String::isBlank)) { "release_notes cannot contain blank entries" }
        val uri = runCatching { URI(downloadUrl) }.getOrNull()
        require(uri?.scheme.equals("https", ignoreCase = true) && !uri?.host.isNullOrBlank()) {
            "download_url must be a valid HTTPS URL"
        }
    }
}

sealed interface UpdateDecision {
    data class UpToDate(val installedVersionCode: Long) : UpdateDecision
    data class Available(val info: AppUpdateInfo, val mandatory: Boolean) : UpdateDecision
}

fun evaluateUpdate(installedVersionCode: Long, info: AppUpdateInfo): UpdateDecision {
    require(installedVersionCode >= 0) { "installedVersionCode must not be negative" }
    return if (info.latestVersionCode <= installedVersionCode) {
        UpdateDecision.UpToDate(installedVersionCode)
    } else {
        UpdateDecision.Available(
            info = info,
            mandatory = info.forceUpdate || installedVersionCode < info.minimumSupportedVersionCode
        )
    }
}
