package com.ourlist.app.data.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CancellationException
import java.security.MessageDigest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val context: Context,
    private val getFirebaseBaseUrl: suspend () -> String
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkForUpdate(currentVersionCode: Int): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val rawBase = getFirebaseBaseUrl().trim().trimEnd('/')
            if (rawBase.isBlank()) return@withContext Result.success(null)

            val url = "$rawBase/app_version.json"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}"))
            }

            val body = response.body?.string()?.trim() ?: return@withContext Result.success(null)
            if (body.isEmpty() || body == "null") return@withContext Result.success(null)

            val info = json.decodeFromString<AppUpdateInfo>(body)
            if (info.latestVersionCode > currentVersionCode) {
                Result.success(info)
            } else {
                Result.success(null)
            }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        apkUrl: String,
        targetFileName: String,
        expectedSha256: String,
        expectedSize: Long,
        expectedVersionCode: Int,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (apkUrl.isBlank()) {
                return@withContext Result.failure(IOException("다운로드 주소가 설정되지 않았습니다."))
            }

            require(targetFileName.matches(Regex("[A-Za-z0-9._-]+\\.apk"))) { "올바른 APK 파일명이 아닙니다." }
            require(expectedSha256.matches(Regex("[a-fA-F0-9]{64}")) && expectedSize > 0) { "업데이트 검증 정보가 없습니다. 배포 정보를 확인해주세요." }
            require(apkUrl.startsWith("https://")) { "보안 다운로드 주소가 필요합니다." }
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(updatesDir, "$targetFileName.pending.apk")

            val request = Request.Builder().url(apkUrl).get().build()
            httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("다운로드 실패: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("다운로드 응답이 비어 있습니다."))
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                        onProgress(progress, totalRead, totalBytes)
                    }
                    output.flush()
                }
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                return@withContext Result.failure(IOException("다운로드된 APK 파일이 비어 있습니다."))
            }

            require(targetFile.length() == expectedSize) { "APK 크기가 배포 정보와 다릅니다." }
            val actualHash = targetFile.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            require(actualHash.equals(expectedSha256, ignoreCase = true)) { "APK 무결성 검증에 실패했습니다." }
            verifyPackage(targetFile, expectedVersionCode)
            val finalFile = File(updatesDir, targetFileName)
            if (finalFile.exists() && !finalFile.delete()) throw IOException("이전 다운로드 파일을 정리할 수 없습니다.")
            check(targetFile.renameTo(finalFile)) { "다운로드 파일을 저장할 수 없습니다." }
            Result.success(finalFile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyPackage(file: File, expectedVersionCode: Int) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: throw IOException("올바른 Android APK가 아닙니다.")
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        require(archive.packageName == context.packageName) { "다른 앱의 APK입니다." }
        val version = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        val current = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        require(version == expectedVersionCode.toLong() && version > current) { "올바른 새 버전 APK가 아닙니다." }
        fun signers(info: android.content.pm.PackageInfo): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            return signatures.orEmpty().map { signature -> MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
        }
        val expected = signers(installed)
        require(expected.isNotEmpty() && signers(archive) == expected) { "기존 앱과 서명이 다릅니다." }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manageIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(manageIntent)
        }
    }

    fun installApk(apkFile: File): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    openInstallPermissionSettings()
                    return Result.failure(IOException("출처를 알 수 없는 앱 설치 권한을 켠 후 [지금 설치하기]를 눌러주세요."))
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishUpdateInfo(info: AppUpdateInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rawBase = getFirebaseBaseUrl().trim().trimEnd('/')
            val url = "$rawBase/app_version.json"
            val bodyStr = json.encodeToString(AppUpdateInfo.serializer(), info)
            val req = Request.Builder()
                .url(url)
                .put(bodyStr.toRequestBody(jsonMediaType))
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(IOException("HTTP ${resp.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
