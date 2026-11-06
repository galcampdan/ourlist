package com.ourlist.app.data.update

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateInfo(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val apkUrl: String = "",
    val apkSha256: String = "",
    val apkSize: Long = 0L,
    val releaseNotes: String = "",
    val minSupportedVersion: Int = 1,
    val forceUpdate: Boolean = false,
    val publishedAt: Long = 0L
)
