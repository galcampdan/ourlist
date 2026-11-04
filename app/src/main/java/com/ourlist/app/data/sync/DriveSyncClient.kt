package com.ourlist.app.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String? = null
)

class DriveSyncClient(
    private val tokenProvider: suspend () -> String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun listFilesInFolder(folderId: String): List<DriveFileMetadata> = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext emptyList()
        val query = "'$folderId' in parents and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,mimeType,modifiedTime)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 404 || response.code == 403) return@withContext emptyList()
                throw IOException("Drive list files failed: ${response.code} ${response.message}")
            }
            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val filesArray = json.optJSONArray("files") ?: JSONArray()
            val list = mutableListOf<DriveFileMetadata>()
            for (i in 0 until filesArray.length()) {
                val item = filesArray.getJSONObject(i)
                list.add(
                    DriveFileMetadata(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        mimeType = item.optString("mimeType", ""),
                        modifiedTime = item.optString("modifiedTime")
                    )
                )
            }
            list
        }
    }

    suspend fun downloadFileContent(fileId: String): String? = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext null
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            response.body?.string()
        }
    }

    suspend fun uploadFile(
        parentFolderId: String,
        fileName: String,
        content: String,
        mimeType: String = "application/json"
    ): String? = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext null

        // 1. Create file metadata
        val metadataJson = JSONObject().apply {
            put("name", fileName)
            put("parents", JSONArray().put(parentFolderId))
            put("mimeType", mimeType)
        }.toString()

        val metaRequest = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files")
            .addHeader("Authorization", "Bearer $token")
            .post(metadataJson.toRequestBody(jsonMediaType))
            .build()

        val fileId = client.newCall(metaRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to create file metadata: ${response.code}")
            val body = response.body?.string() ?: return@withContext null
            JSONObject(body).getString("id")
        }

        // 2. Upload content
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer $token")
            .patch(content.toRequestBody(mimeType.toMediaType()))
            .build()

        client.newCall(uploadRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to upload content: ${response.code}")
        }
        fileId
    }

    suspend fun updateFileContent(
        fileId: String,
        content: String,
        mimeType: String = "application/json"
    ) = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
        val uploadRequest = Request.Builder()
            .url(uploadUrl)
            .addHeader("Authorization", "Bearer $token")
            .patch(content.toRequestBody(mimeType.toMediaType()))
            .build()

        client.newCall(uploadRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to update file: ${response.code}")
        }
    }

    suspend fun createFolder(name: String, parentFolderId: String? = null): String? = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext null
        val metadata = JSONObject().apply {
            put("name", name)
            put("mimeType", "application/vnd.google-apps.folder")
            if (parentFolderId != null) {
                put("parents", JSONArray().put(parentFolderId))
            }
        }.toString()

        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files")
            .addHeader("Authorization", "Bearer $token")
            .post(metadata.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            JSONObject(body).getString("id")
        }
    }
}
