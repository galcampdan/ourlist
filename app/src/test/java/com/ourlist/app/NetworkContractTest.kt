package com.ourlist.app

import com.ourlist.app.data.sync.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class NetworkContractTest {
    private fun mock(target:Any, answer:(Request)->Triple<Int,String,String?>) {
        val client=OkHttpClient.Builder().addInterceptor { chain ->
            val (status,body,etag)=answer(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("test")
                .apply {if(etag!=null) header("ETag",etag)}.body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        target.javaClass.getDeclaredField("client").apply {isAccessible=true}.set(target,client)
    }
    @Test fun occupiedRoomRejectsThirdDeviceWithoutWriting()=runBlocking {
        val client=CoupleSyncClient(sessionProvider={FirebaseIdentity("test-user","test-id-token")}) {"https://ourlist-4462e-default-rtdb.firebaseio.com"};var writes=0
        mock(client) {request ->if(request.method!="GET") writes++;Triple(200,"{\"deviceA\":\"A\",\"deviceB\":\"B\",\"ownerUid\":\"owner-A\",\"partnerUid\":\"partner-B\"}","tag")}
        assertTrue(runCatching {client.joinRoom("123456","C","third")}.isFailure);assertEquals(0,writes)
    }
    @Test fun existingRoomCannotBeOverwritten()=runBlocking {
        val client=CoupleSyncClient(sessionProvider={FirebaseIdentity("test-user","test-id-token")}) {"https://ourlist-4462e-default-rtdb.firebaseio.com"};var writes=0
        mock(client) {request ->if(request.method!="GET") writes++;Triple(200,"{\"meta\":{\"deviceA\":\"existing\"}}","tag")}
        assertTrue(runCatching {client.createRoom("123456","A","name",emptyList(),emptyList())}.exceptionOrNull() is RoomCodeInUseException);assertEquals(0,writes)
    }
    @Test fun concurrentCodeClaimFailsConditionalWrite()=runBlocking {
        val client=CoupleSyncClient(sessionProvider={FirebaseIdentity("test-user","test-id-token")}) {"https://ourlist-4462e-default-rtdb.firebaseio.com"};var conditional=false
        mock(client) {request ->if(request.method=="GET") Triple(200,"null","empty-tag") else {conditional=request.header("if-match")=="empty-tag";Triple(412,"{}",null)}}
        assertTrue(runCatching {client.createRoom("123456","A","name",emptyList(),emptyList())}.isFailure);assertTrue(conditional)
    }
    @Test fun serverReadFailureIsNotEmptyHistory()=runBlocking {
        val client=CoupleSyncClient(sessionProvider={FirebaseIdentity("test-user","test-id-token")}) {"https://ourlist-4462e-default-rtdb.firebaseio.com"}
        mock(client) {Triple(403,"{\"error\":\"denied\"}",null)}
        assertTrue(runCatching {client.fetchOperations("123456")}.isFailure)
    }
    @Test fun malformedHistoryIsNotAcknowledgedAsEmpty()=runBlocking {
        val client=CoupleSyncClient(sessionProvider={FirebaseIdentity("test-user","test-id-token")}) {"https://ourlist-4462e-default-rtdb.firebaseio.com"}
        mock(client) {Triple(200,"{\"bad\":{\"unexpected\":true}}",null)}
        assertTrue(runCatching {client.fetchOperations("123456")}.isFailure)
    }
    @Test fun driveFollowsEveryPage()=runBlocking {
        val client=DriveSyncClient {"test-token"};var count=0
        mock(client) {req ->count++; if(req.url.queryParameter("pageToken")==null)
            Triple(200,"{\"files\":[{\"id\":\"1\",\"name\":\"one\"}],\"nextPageToken\":\"next\"}",null)
            else Triple(200,"{\"files\":[{\"id\":\"2\",\"name\":\"two\"}]}",null)}
        assertEquals(listOf("1","2"),client.listFilesInFolder("folder").map {it.id});assertEquals(2,count)
    }
    @Test fun appDataListUsesCorrectSpace()=runBlocking {
        val client=DriveSyncClient {"token"};var space=""
        mock(client) {req ->space=req.url.queryParameter("spaces").orEmpty();Triple(200,"{\"files\":[]}",null)}
        client.listFilesInFolder("appDataFolder");assertEquals("appDataFolder",space)
    }
    @Test fun missingDriveTokenCannotLookLikeUploadedFile()=runBlocking {
        assertTrue(runCatching {DriveSyncClient {null}.uploadFile("folder","op.json","{}")} .isFailure)
    }
    @Test fun unauthorizedDriveRequestRefreshesTokenOnce()=runBlocking {
        var token="expired";var invalidations=0
        val client=DriveSyncClient(tokenInvalidator={invalidations++;token="fresh"}) {token}
        mock(client) {req ->if(req.header("Authorization")=="Bearer expired") Triple(401,"{}",null) else Triple(200,"{\"files\":[]}",null)}
        client.listFilesInFolder("folder");assertEquals(1,invalidations)
    }
    @Test fun operationUploadUsesSingleMultipartRequest()=runBlocking {
        val client=DriveSyncClient {"token"};var count=0
        mock(client) {req -> count++;assertEquals("multipart",req.url.queryParameter("uploadType"));assertTrue(req.body!!.contentType().toString().startsWith("multipart/related"));Triple(200,"{\"id\":\"uploaded\"}",null)}
        assertEquals("uploaded",client.uploadFile("folder","op.json","{}"));assertEquals(1,count)
    }
}
