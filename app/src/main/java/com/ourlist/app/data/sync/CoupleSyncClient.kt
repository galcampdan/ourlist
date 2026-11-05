package com.ourlist.app.data.sync

import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.SyncOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class RoomMeta(
    val createdAt: Long = System.currentTimeMillis(),
    val deviceA: String = "",
    val userAName: String = "나",
    val deviceB: String = "",
    val userBName: String = "상대방"
)

@Serializable
data class RoomStateData(
    val shared: List<BucketItem> = emptyList(),
    val heart: List<BucketItem> = emptyList(),
    val lastUpdated: Long = 0L,
    val lastUpdatedBy: String = ""
)

class CoupleSyncClient(
    private val baseUrlProvider: suspend () -> String = { "https://ourlist-4462e-default-rtdb.firebaseio.com" }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private suspend fun getCleanBaseUrl(): String {
        var base = baseUrlProvider().trim()
        if (base.endsWith("/")) {
            base = base.substring(0, base.length - 1)
        }
        return base
    }

    suspend fun checkRoomExists(roomCode: String): Boolean = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()
        val url = "$base/rooms/$roomCode/meta.json"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body?.string()?.trim() ?: return@withContext false
                body.isNotEmpty() && body != "null"
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchMeta(roomCode: String): RoomMeta? = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()
        val url = "$base/rooms/$roomCode/meta.json"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string()?.trim() ?: return@withContext null
            if (body.isEmpty() || body == "null") return@withContext null
            try {
                json.decodeFromString<RoomMeta>(body)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun createRoom(
        roomCode: String,
        myDeviceId: String,
        myName: String,
        sharedItems: List<BucketItem>,
        heartItems: List<BucketItem>
    ): Boolean = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()
        val now = System.currentTimeMillis()

        // 1. Put Meta
        val meta = RoomMeta(
            createdAt = now,
            deviceA = myDeviceId,
            userAName = myName.ifBlank { "나" }
        )
        val metaBody = json.encodeToString(meta).toRequestBody(jsonMediaType)
        val metaReq = Request.Builder()
            .url("$base/rooms/$roomCode/meta.json")
            .put(metaBody)
            .build()
        client.newCall(metaReq).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("커플 방 메타 생성 실패: ${resp.code} ${resp.message}")
        }

        // 2. Put State
        val state = RoomStateData(
            shared = sharedItems,
            heart = heartItems,
            lastUpdated = now,
            lastUpdatedBy = myDeviceId
        )
        val stateBody = json.encodeToString(state).toRequestBody(jsonMediaType)
        val stateReq = Request.Builder()
            .url("$base/rooms/$roomCode/state.json")
            .put(stateBody)
            .build()
        client.newCall(stateReq).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("커플 방 초기 데이터 업로드 실패: ${resp.code} ${resp.message}")
        }
        true
    }

    suspend fun joinRoom(
        roomCode: String,
        myDeviceId: String,
        myName: String
    ): RoomStateData = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()

        // 1. Check meta
        val meta = fetchMeta(roomCode)
            ?: throw IOException("존재하지 않는 커플 코드입니다. 코드를 다시 확인해주세요.")

        // 2. Patch deviceB info
        val patchObj = JSONObject().apply {
            put("deviceB", myDeviceId)
            put("userBName", myName.ifBlank { "상대방" })
        }
        val patchReq = Request.Builder()
            .url("$base/rooms/$roomCode/meta.json")
            .patch(patchObj.toString().toRequestBody(jsonMediaType))
            .build()
        client.newCall(patchReq).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("커플 연결 업데이트 실패: ${resp.code}")
        }

        // 3. Fetch state
        fetchState(roomCode) ?: RoomStateData()
    }

    suspend fun pushOperation(roomCode: String, op: SyncOperation): Boolean = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()
        val opBody = json.encodeToString(op).toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url("$base/rooms/$roomCode/operations/${op.opId}.json")
            .put(opBody)
            .build()
        client.newCall(req).execute().use { resp ->
            resp.isSuccessful
        }
    }

    suspend fun fetchOperations(roomCode: String): List<SyncOperation> = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()
        val req = Request.Builder()
            .url("$base/rooms/$roomCode/operations.json")
            .get()
            .build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string()?.trim() ?: return@withContext emptyList()
            if (body.isEmpty() || body == "null") return@withContext emptyList()

            try {
                val jsonObject = JSONObject(body)
                val list = mutableListOf<SyncOperation>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val opJsonStr = jsonObject.getJSONObject(key).toString()
                    try {
                        val op = json.decodeFromString<SyncOperation>(opJsonStr)
                        list.add(op)
                    } catch (e: Exception) {
                        // ignore malformed op
                    }
                }
                list.sortedBy { it.createdAt }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun pushState(
        roomCode: String,
        myDeviceId: String,
        sharedItems: List<BucketItem>,
        heartItems: List<BucketItem>
    ): Boolean = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()
        val state = RoomStateData(
            shared = sharedItems,
            heart = heartItems,
            lastUpdated = System.currentTimeMillis(),
            lastUpdatedBy = myDeviceId
        )
        val stateBody = json.encodeToString(state).toRequestBody(jsonMediaType)
        val req = Request.Builder()
            .url("$base/rooms/$roomCode/state.json")
            .put(stateBody)
            .build()
        client.newCall(req).execute().use { resp ->
            resp.isSuccessful
        }
    }

    suspend fun fetchState(roomCode: String): RoomStateData? = withContext(Dispatchers.IO) {
        val base = getCleanBaseUrl()
        val req = Request.Builder()
            .url("$base/rooms/$roomCode/state.json")
            .get()
            .build()
        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string()?.trim() ?: return@withContext null
            if (body.isEmpty() || body == "null") return@withContext null
            try {
                json.decodeFromString<RoomStateData>(body)
            } catch (e: Exception) {
                null
            }
        }
    }
}
