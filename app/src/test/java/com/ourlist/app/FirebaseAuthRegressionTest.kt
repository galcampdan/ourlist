package com.ourlist.app

import com.ourlist.app.data.sync.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class FirebaseAuthRegressionTest {
    private class Store(var value: FirebaseIdentity? = null): FirebaseSessionStore {
        var saves=0
        override fun load()=value
        override fun save(identity: FirebaseIdentity) {value=identity;saves++}
    }
    private fun client(answer:(Request)->Pair<Int,String>)=OkHttpClient.Builder().addInterceptor { chain ->
        val (status,body)=answer(chain.request())
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("test")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()
    @Test fun storedSessionDoesNotCreateNewIdentity()=runBlocking {
        val original=FirebaseIdentity("same-user","valid","refresh")
        val session=FirebaseSession("key",Store(original),client {error("unnecessary network")})
        assertSame(original,session.identity())
    }
    @Test fun initialLoginPersistsRefreshCredential()=runBlocking {
        val store=Store();var calls=0
        val session=FirebaseSession("key",store,client { req ->
            calls++;assertEquals("/v1/accounts:signUp",req.url.encodedPath)
            200 to """{"localId":"uid","idToken":"id","refreshToken":"refresh","expiresIn":"3600"}"""
        })
        assertEquals("uid",session.identity().uid);session.identity()
        assertEquals(1,calls);assertEquals(1,store.saves);assertEquals("refresh",store.value!!.refreshToken)
    }
    @Test fun expiredSessionRefreshesSameUid()=runBlocking {
        val store=Store(FirebaseIdentity("uid","old","refresh",0))
        val session=FirebaseSession("key",store,client { req ->
            assertEquals("securetoken.googleapis.com",req.url.host)
            200 to """{"user_id":"uid","id_token":"new","refresh_token":"next","expires_in":"3600"}"""
        })
        assertEquals("new",session.identity().idToken);assertEquals("uid",store.value!!.uid)
    }
    @Test fun failedRefreshDoesNotSilentlyReplaceMemberAccount()=runBlocking {
        val store=Store(FirebaseIdentity("uid","old","refresh",0));var calls=0
        val session=FirebaseSession("key",store,client { calls++;401 to "{}" })
        assertTrue(runCatching {session.identity()}.isFailure);assertEquals(1,calls);assertEquals(0,store.saves);assertEquals("uid",store.value!!.uid)
    }
    @Test fun refreshUidMismatchIsRejected()=runBlocking {
        val store=Store(FirebaseIdentity("uid","old","refresh",0))
        val session=FirebaseSession("key",store,client {200 to """{"user_id":"different","id_token":"new","refresh_token":"next","expires_in":"3600"}"""})
        assertTrue(runCatching {session.identity()}.isFailure);assertEquals(0,store.saves)
    }
    @Test fun permission401StillSubmitsRequestWithoutReadingSharedData()=runBlocking {
        val requests=mutableListOf<Request>()
        val target=CoupleSyncClient(sessionProvider={FirebaseIdentity("requester","token")})
        val network=client {req ->
            requests+=req;assertEquals("token",req.url.queryParameter("auth"))
            when {
                req.url.encodedPath.endsWith("/meta.json") ->401 to "{}"
                req.method=="GET" ->200 to "null"
                else ->200 to "{}"
            }
        }
        target.javaClass.getDeclaredField("client").apply {isAccessible=true}.set(target,network)
        assertTrue(runCatching {target.joinRoom("123456","B","partner")}.exceptionOrNull() is ApprovalRequiredException)
        assertTrue(requests.any {it.method=="PUT" && it.url.encodedPath=="/pairRequests/123456/requester.json"})
        assertFalse(requests.any {it.url.encodedPath.endsWith("/state.json")})
    }
    @Test fun authorizationRetryUsesRefreshedTokenOnce()=runBlocking {
        var refreshes=0;var calls=0
        val target=CoupleSyncClient(sessionProvider={ force -> if(force) refreshes++;FirebaseIdentity("uid",if(force) "fresh" else "old")})
        val network=client {req ->calls++; if(req.url.queryParameter("auth")=="old") 401 to "{}" else 200 to "null"}
        target.javaClass.getDeclaredField("client").apply {isAccessible=true}.set(target,network)
        assertNull(target.fetchMeta("123456"));assertEquals(1,refreshes);assertEquals(2,calls)
    }
}
