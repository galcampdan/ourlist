package com.ourlist.app.data.sync

import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.SyncOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

@Serializable
data class RoomMeta(
    val createdAt: Long = System.currentTimeMillis(),
    val deviceA: String = "", val userAName: String = "나",
    val deviceB: String = "", val userBName: String = "상대방",
    val datingStartDate: String = "", val closed: Boolean = false,
    val ownerUid: String = "", val partnerUid: String = "", val protocolVersion: Int = 2
) {
    val isBothConnected: Boolean get() = !closed && ownerUid.isNotBlank() && partnerUid.isNotBlank() && ownerUid != partnerUid && deviceA.isNotBlank() && deviceB.isNotBlank() && deviceA != deviceB
    fun isMember(deviceId: String) = !closed && deviceId.isNotBlank() && deviceId in setOf(deviceA, deviceB)
}
@Serializable
data class RoomStateData(val shared: List<BucketItem> = emptyList(), val heart: List<BucketItem> = emptyList(), val lastUpdated: Long = 0L, val lastUpdatedBy: String = "", val appliedOperationIds: List<String> = emptyList())
@Serializable
private data class InitialRoom(val meta: RoomMeta, val state: RoomStateData)
@Serializable
data class JoinRequest(val deviceId: String, val userName: String, val confirmationCode: String, val createdAt: Long = System.currentTimeMillis())
class ApprovalRequiredException : IOException("상대방 휴대폰에서 확인번호를 대조하고 연결을 승인해주세요.")
class RoomCodeInUseException : IOException("이미 사용 중인 커플 코드입니다.")
class SyncHttpException(val status: Int) : IOException(if (status == 401 || status == 403) "접근 권한이 없습니다. 연결 승인과 커플 코드를 확인해주세요." else "커플 연동 요청 실패 (HTTP $status)")

class CoupleSyncClient(
    private val sessionProvider: suspend (Boolean) -> FirebaseIdentity = { throw IOException("Firebase 기기 인증 설정이 필요합니다.") },
    private val baseUrlProvider: suspend () -> String = { "https://ourlist-4462e-default-rtdb.firebaseio.com" }
) {
    private val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).build()
    private val media = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    suspend fun currentUid(): String = sessionProvider(false).uid
    private suspend fun url(code: String, path: String = "", requests: Boolean = false): String {
        require(code.matches(Regex("[0-9]{6}"))) { "6자리 커플 코드가 필요합니다." }
        val base = baseUrlProvider().trim().trimEnd('/')
        require(base == "https://ourlist-4462e-default-rtdb.firebaseio.com") { "이 버전은 OurList의 보안 서버에 연결해야 합니다." }
        return "$base/${if (requests) "pairRequests" else "privateRooms"}/$code$path.json"
    }
    private suspend fun read(request: Request): Pair<String, String?> {
        fun authorized(identity: FirebaseIdentity) = request.newBuilder().url(request.url.newBuilder().setQueryParameter("auth", identity.idToken).build()).build()
        var response = client.newCall(authorized(sessionProvider(false))).execute()
        if (response.code == 401) {
            response.close()
            response = client.newCall(authorized(sessionProvider(true))).execute()
        }
        return response.use {
            if (it.code == 412) throw RoomCodeInUseException()
            if (!it.isSuccessful) throw SyncHttpException(it.code)
            (it.body?.string() ?: throw IOException("연동 응답이 비어 있습니다.")) to it.header("ETag")
        }
    }
    private suspend fun metaWithTag(code: String): Pair<RoomMeta?, String?> {
        val (body, tag) = read(Request.Builder().url(url(code, "/meta")).header("X-Firebase-ETag", "true").build())
        return (if (body.trim() == "null") null else json.decodeFromString<RoomMeta>(body)) to tag
    }
    suspend fun fetchMeta(roomCode: String): RoomMeta? = withContext(Dispatchers.IO) { metaWithTag(roomCode).first }
    private suspend fun putConditional(address: String, tag: String, content: String) {
        read(Request.Builder().url(address).header("if-match", tag).put(content.toRequestBody(media)).build())
    }
    suspend fun createRoom(roomCode: String, myDeviceId: String, myName: String, sharedItems: List<BucketItem>, heartItems: List<BucketItem>): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val address = url(roomCode)
        val pair = try { read(Request.Builder().url(address).header("X-Firebase-ETag", "true").build()) }
            catch (e: SyncHttpException) { if (e.status == 401 || e.status == 403) throw RoomCodeInUseException() else throw e }
        if (pair.first.trim() != "null") throw RoomCodeInUseException()
        val now = System.currentTimeMillis()
        val initial = InitialRoom(RoomMeta(createdAt = now, deviceA = myDeviceId, userAName = myName.ifBlank { "나" }.take(80), ownerUid = uid), RoomStateData(sharedItems, heartItems, now, myDeviceId))
        putConditional(address, pair.second ?: throw IOException("서버가 안전한 코드 생성을 지원하지 않습니다."), json.encodeToString(initial))
        true
    }
    suspend fun ownJoinRequest(roomCode: String): JoinRequest? = withContext(Dispatchers.IO) {
        val (body, _) = read(Request.Builder().url(url(roomCode, "/${currentUid()}", true)).build())
        if (body.trim() == "null") null else json.decodeFromString<JoinRequest>(body)
    }
    suspend fun joinRoom(roomCode: String, myDeviceId: String, myName: String): RoomStateData = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val meta = try { metaWithTag(roomCode).first ?: throw IOException("존재하지 않는 커플 코드입니다.") }
            catch (e: SyncHttpException) { if (e.status == 401 || e.status == 403) null else throw e }
        if (meta != null) {
            require(!meta.closed) { "해제된 커플 코드입니다. 새 코드를 발급해주세요." }
            require(meta.ownerUid != uid) { "내가 생성한 코드입니다. 상대방 기기에서 입력해주세요." }
            if (meta.partnerUid == uid && meta.deviceB == myDeviceId) return@withContext fetchState(roomCode) ?: throw IOException("공유 데이터를 불러오지 못했습니다.")
            require(meta.partnerUid.isBlank()) { "이미 두 기기가 연결된 코드입니다." }
        }
        if (ownJoinRequest(roomCode) == null) {
            val request = JoinRequest(myDeviceId, myName.ifBlank { "상대방" }.take(80), SecureRandom().nextInt(1000000).toString().padStart(6, '0'))
            read(Request.Builder().url(url(roomCode, "/$uid", true)).put(json.encodeToString(request).toRequestBody(media)).build())
        }
        throw ApprovalRequiredException()
    }
    suspend fun pendingJoinRequests(roomCode: String): Map<String, JoinRequest> = withContext(Dispatchers.IO) {
        val (body, _) = read(Request.Builder().url(url(roomCode, requests = true)).build())
        if (body.trim() == "null") emptyMap() else json.decodeFromString<Map<String, JoinRequest>>(body)
    }
    suspend fun approveJoin(roomCode: String, requesterUid: String) = withContext(Dispatchers.IO) {
        require(requesterUid.matches(Regex("[A-Za-z0-9_-]{1,128}")))
        val (meta, tag) = metaWithTag(roomCode)
        require(meta != null && !meta.closed && meta.ownerUid == currentUid() && meta.partnerUid.isBlank()) { "연결을 승인할 권한이 없거나 이미 연결된 방입니다." }
        val (body, _) = read(Request.Builder().url(url(roomCode, "/$requesterUid", true)).build())
        require(body.trim() != "null") { "연결 요청이 취소되었습니다." }
        val request = json.decodeFromString<JoinRequest>(body)
        require(request.deviceId != meta.deviceA)
        putConditional(url(roomCode, "/meta"), tag ?: throw IOException("연결 확인 응답이 없습니다."), json.encodeToString(meta.copy(partnerUid = requesterUid, deviceB = request.deviceId, userBName = request.userName)))
    }
    suspend fun rejectJoin(roomCode: String, requesterUid: String) = withContext(Dispatchers.IO) {
        require(requesterUid.matches(Regex("[A-Za-z0-9_-]{1,128}")))
        read(Request.Builder().url(url(roomCode, "/$requesterUid", true)).delete().build()); Unit
    }
    suspend fun closeRoom(roomCode: String, myDeviceId: String) = withContext(Dispatchers.IO) {
        val (meta, tag) = try { metaWithTag(roomCode) } catch (e: SyncHttpException) { if (e.status == 401 || e.status == 403) null to null else throw e }
        val uid = currentUid()
        if (meta == null || uid !in setOf(meta.ownerUid, meta.partnerUid)) {
            rejectJoin(roomCode, uid)
        } else {
            if (meta.closed) return@withContext
            require(meta.isMember(myDeviceId)) { "이 기기의 연결 권한을 확인할 수 없습니다." }
            putConditional(url(roomCode, "/meta"), tag ?: throw IOException("연결 확인 응답이 없습니다."), json.encodeToString(meta.copy(closed = true)))
        }
    }
    suspend fun pushOperation(roomCode: String, op: SyncOperation): Boolean = withContext(Dispatchers.IO) {
        require(op.opId.matches(Regex("[A-Za-z0-9_-]+")))
        read(Request.Builder().url(url(roomCode, "/operations/${op.opId}")).put(json.encodeToString(op).toRequestBody(media)).build()); true
    }
    suspend fun fetchOperations(roomCode: String): List<SyncOperation> = withContext(Dispatchers.IO) {
        val (body, _) = read(Request.Builder().url(url(roomCode, "/operations")).build())
        if (body.trim() == "null") emptyList() else orderedOperations(json.decodeFromString<Map<String, SyncOperation>>(body).values.toList())
    }
    suspend fun pushState(roomCode: String, myDeviceId: String, sharedItems: List<BucketItem>, heartItems: List<BucketItem>, appliedOperationIds: List<String> = emptyList()): Boolean = withContext(Dispatchers.IO) {
        read(Request.Builder().url(url(roomCode, "/state")).put(json.encodeToString(RoomStateData(sharedItems, heartItems, System.currentTimeMillis(), myDeviceId, appliedOperationIds)).toRequestBody(media)).build()); true
    }
    suspend fun fetchState(roomCode: String): RoomStateData? = withContext(Dispatchers.IO) {
        val (body, _) = read(Request.Builder().url(url(roomCode, "/state")).build())
        if (body.trim() == "null") null else json.decodeFromString<RoomStateData>(body)
    }
    suspend fun updateDatingStartDate(roomCode: String, date: String): Boolean = withContext(Dispatchers.IO) {
        val (meta, tag) = metaWithTag(roomCode)
        require(meta != null && !meta.closed)
        putConditional(url(roomCode, "/meta"), tag ?: throw IOException("연결 확인 응답이 없습니다."), json.encodeToString(meta.copy(datingStartDate = date))); true
    }
}
