package com.ourlist.app.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DriveFileMetadata(val id: String, val name: String, val mimeType: String, val modifiedTime: String? = null)

class DriveSyncClient(
    private val tokenInvalidator: suspend (String) -> Unit = {},
    private val tokenProvider: suspend () -> String?
) {
    companion object {
        const val OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive"
    }
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun execute(build: (String) -> Request): Response {
        var token = tokenProvider()?.takeIf { it.isNotBlank() } ?: throw IOException("Google Drive 로그인이 필요합니다.")
        var response = client.newCall(build(token)).execute()
        if (response.code == 401) {
            response.close()
            tokenInvalidator(token)
            token = tokenProvider()?.takeIf { it.isNotBlank() } ?: throw IOException("Google Drive에 다시 로그인해주세요.")
            response = client.newCall(build(token)).execute()
        }
        if (!response.isSuccessful) {
            val status = response.code
            response.close()
            throw IOException("Google Drive 요청 실패 (HTTP $status). 계정과 폴더 접근 권한을 확인해주세요.")
        }
        return response
    }

    suspend fun listFilesInFolder(folderId: String): List<DriveFileMetadata> = withContext(Dispatchers.IO) {
        require(folderId == "appDataFolder" || folderId.matches(Regex("[A-Za-z0-9_-]+"))) { "올바른 Drive 폴더 ID가 아닙니다." }
        val files = mutableListOf<DriveFileMetadata>()
        var next: String? = null
        val seen = mutableSetOf<String>()
        do {
            val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
                .addQueryParameter("q", "'$folderId' in parents and trashed = false")
                .addQueryParameter("fields", "nextPageToken,files(id,name,mimeType,modifiedTime)")
                .addQueryParameter("pageSize", "1000")
                .apply {
                    if (folderId == "appDataFolder") addQueryParameter("spaces", "appDataFolder")
                    if (next != null) addQueryParameter("pageToken", next)
                }.build()
            execute { token -> Request.Builder().url(url).header("Authorization", "Bearer $token").build() }.use { response ->
                val body = JSONObject(response.body?.string() ?: throw IOException("Drive 응답이 비어 있습니다."))
                val page = body.optJSONArray("files") ?: JSONArray()
                for (i in 0 until page.length()) {
                    val f = page.getJSONObject(i)
                    files.add(DriveFileMetadata(f.getString("id"), f.getString("name"), f.optString("mimeType"), f.optString("modifiedTime")))
                }
                next = body.optString("nextPageToken").takeIf { it.isNotBlank() }
                if (next != null && !seen.add(next!!)) throw IOException("Drive 페이지 응답이 반복되어 중단했습니다.")
            }
        } while (next != null)
        files.distinctBy { it.id }
    }

    suspend fun downloadFileContent(fileId: String): String = withContext(Dispatchers.IO) {
        execute { token -> Request.Builder().url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token").build() }.use { it.body?.string() ?: throw IOException("Drive 파일이 비어 있습니다.") }
    }

    suspend fun uploadFile(parentFolderId: String, fileName: String, content: String, mimeType: String = "application/json"): String = withContext(Dispatchers.IO) {
        val metadata = JSONObject().put("name", fileName).put("parents", JSONArray().put(parentFolderId)).put("mimeType", mimeType)
        val multipart = okhttp3.MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toString().toRequestBody(jsonMediaType))
            .addPart(content.toRequestBody(mimeType.toMediaType())).build()
        execute { token -> Request.Builder().url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .header("Authorization", "Bearer $token").post(multipart).build() }.use {
            JSONObject(it.body?.string() ?: throw IOException("Drive 파일 생성 응답이 비어 있습니다.")).getString("id")
        }
    }

    suspend fun updateFileContent(fileId: String, content: String, mimeType: String = "application/json") = withContext(Dispatchers.IO) {
        execute { token -> Request.Builder().url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
            .header("Authorization", "Bearer $token").patch(content.toRequestBody(mimeType.toMediaType())).build() }.close()
    }

    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        execute { token -> Request.Builder().url("https://www.googleapis.com/drive/v3/files/$fileId")
            .header("Authorization", "Bearer $token").delete().build() }.close()
    }

    suspend fun createFolder(name: String, parentFolderId: String? = null): String = withContext(Dispatchers.IO) {
        val metadata = JSONObject().put("name", name).put("mimeType", "application/vnd.google-apps.folder")
        if (parentFolderId != null) metadata.put("parents", JSONArray().put(parentFolderId))
        execute { token -> Request.Builder().url("https://www.googleapis.com/drive/v3/files")
            .header("Authorization", "Bearer $token").post(metadata.toString().toRequestBody(jsonMediaType)).build() }.use {
            JSONObject(it.body?.string() ?: throw IOException("Drive 폴더 응답이 비어 있습니다.")).getString("id")
        }
    }
}
