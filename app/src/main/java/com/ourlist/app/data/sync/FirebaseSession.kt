package com.ourlist.app.data.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class FirebaseIdentity(val uid: String, val idToken: String, val refreshToken: String = "", val expiresAt: Long = Long.MAX_VALUE) {
    override fun toString() = "FirebaseIdentity(uid=$uid)"
}
interface FirebaseSessionStore {
    fun load(): FirebaseIdentity?
    fun save(identity: FirebaseIdentity)
}
class AndroidFirebaseSessionStore(context: Context) : FirebaseSessionStore {
    private val prefs = context.getSharedPreferences("firebase_device_session", Context.MODE_PRIVATE)
    override fun load(): FirebaseIdentity? {
        val uid = prefs.getString("uid", null) ?: return null
        return FirebaseIdentity(uid, prefs.getString("idToken", "").orEmpty(), prefs.getString("refreshToken", "").orEmpty(), prefs.getLong("expiresAt", 0))
    }
    override fun save(identity: FirebaseIdentity) {
        if (!prefs.edit().putString("uid", identity.uid).putString("idToken", identity.idToken)
            .putString("refreshToken", identity.refreshToken).putLong("expiresAt", identity.expiresAt).commit()) throw IOException("기기 인증 정보를 보관하지 못했습니다.")
    }
}
class FirebaseSession(
    private val apiKey: String,
    private val store: FirebaseSessionStore,
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(20, java.util.concurrent.TimeUnit.SECONDS).build()
) {
    private val mutex = Mutex()
    private var cached: FirebaseIdentity? = null
    suspend fun identity(forceRefresh: Boolean = false): FirebaseIdentity = withContext(Dispatchers.IO) {
        mutex.withLock {
            require(apiKey.isNotBlank()) { "Firebase 기기 인증 설정이 필요합니다." }
            val previous = cached ?: store.load()
            if (!forceRefresh && previous != null && previous.idToken.isNotBlank() && previous.expiresAt > System.currentTimeMillis() + 60_000) return@withLock previous.also { cached = it }
            val request = if (previous?.refreshToken?.isNotBlank() == true) {
                Request.Builder().url("https://securetoken.googleapis.com/v1/token".toHttpUrl().newBuilder().addQueryParameter("key", apiKey).build())
                    .post(FormBody.Builder().add("grant_type", "refresh_token").add("refresh_token", previous.refreshToken).build()).build()
            } else {
                Request.Builder().url("https://identitytoolkit.googleapis.com/v1/accounts:signUp".toHttpUrl().newBuilder().addQueryParameter("key", apiKey).build())
                    .post("{\"returnSecureToken\":true}".toRequestBody("application/json".toMediaType())).build()
            }
            val identity = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("기기 인증 실패 (HTTP ${response.code}). 익명 로그인 설정과 네트워크를 확인해주세요.")
                val data = JSONObject(response.body?.string() ?: throw IOException("인증 응답이 비어 있습니다."))
                val uid = if (previous?.refreshToken?.isNotBlank() == true) data.getString("user_id") else data.getString("localId")
                if (previous != null && previous.uid != uid) throw IOException("기기 인증 계정이 일치하지 않습니다.")
                val seconds = data.optString("expires_in", data.optString("expiresIn")).toLongOrNull() ?: throw IOException("인증 만료 정보를 확인할 수 없습니다.")
                FirebaseIdentity(uid, data.optString("id_token", data.optString("idToken")), data.optString("refresh_token", data.optString("refreshToken")), System.currentTimeMillis() + seconds.coerceIn(60, 86400) * 1000)
            }
            require(identity.uid.isNotBlank() && identity.idToken.isNotBlank() && identity.refreshToken.isNotBlank()) { "유효하지 않은 인증 응답입니다." }
            store.save(identity)
            cached = identity
            identity
        }
    }
}
