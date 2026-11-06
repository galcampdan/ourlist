import com.ourlist.app.data.backup.CryptoEngine
import com.ourlist.app.data.sync.CoupleSyncClient
import com.ourlist.app.data.sync.DriveSyncClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class AdversarialAuditTest {
    private fun intercept(target: Any, responder: (okhttp3.Request) -> Pair<Int, String>) {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val (code, body) = responder(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("audit mock").body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        target.javaClass.getDeclaredField("client").apply { isAccessible = true }.set(target, client)
    }
    @Test fun manualBackupCannotBeRecoveredUsingSeparateRecoveryCode() {
        val code = CryptoEngine.generateRecoveryCode()
        val encrypted = CryptoEngine.encrypt("[{\"id\":\"audit\"}]", "manual-password")
        assertEquals("[{\"id\":\"audit\"}]", CryptoEngine.decrypt(encrypted, "manual-password"))
        val failure = runCatching { CryptoEngine.decrypt(encrypted, code) }.exceptionOrNull()
        assertNotNull("Separately issued recovery code does not unlock manual backup", failure)
        println("CONFIRMED: actual CryptoEngine rejects recovery code for password-encrypted manual backup")
    }
    @Test fun thirdDeviceCanReplaceOccupiedPartnerInClient() = runBlocking {
        val client = CoupleSyncClient { "https://audit.invalid" }
        var patch = ""
        val requests = mutableListOf<String>()
        intercept(client) { req ->
            requests.add(req.method + " " + req.url.encodedPath)
            assertNull("Client sends no authenticated identity", req.header("Authorization"))
            when {
                req.method == "GET" && req.url.encodedPath.endsWith("/meta.json") -> 200 to "{\"deviceA\":\"owner-A\",\"deviceB\":\"partner-B\"}"
                req.method == "PATCH" -> { patch = Buffer().also { req.body!!.writeTo(it) }.readUtf8(); 200 to patch }
                else -> 200 to "{\"shared\":[],\"heart\":[]}"
            }
        }
        client.joinRoom("123456", "third-device-C", "audit third")
        assertTrue(patch.contains("third-device-C"))
        assertEquals(3, requests.size)
        println("CONFIRMED: occupied partner-B replaced by third-device-C; no Authorization header (mock server)")
    }
    @Test fun createRoomDoesNotCheckExistingRoomBeforeUnconditionalPut() = runBlocking {
        val client = CoupleSyncClient { "https://audit.invalid" }
        val requests = mutableListOf<String>()
        intercept(client) { req ->
            requests.add(req.method)
            assertNull(req.header("If-Match"))
            200 to "{}"
        }
        assertTrue(client.createRoom("123456", "new-owner", "audit", emptyList(), emptyList()))
        assertEquals(listOf("PUT", "PUT"), requests)
        println("CONFIRMED: room creation unconditionally PUTs both meta and state, without collision guard")
    }
    @Test fun forbiddenOperationsReadLooksLikeEmptyRemoteHistory() = runBlocking {
        val client = CoupleSyncClient { "https://audit.invalid" }
        intercept(client) { 403 to "{\"error\":\"Permission denied\"}" }
        assertTrue(client.fetchOperations("123456").isEmpty())
        println("CONFIRMED: remote operations HTTP 403 is returned as an empty list")
    }
    @Test fun driveHistorySilentlyDropsNextPage() = runBlocking {
        val client = DriveSyncClient { "audit-token" }
        var count = 0
        intercept(client) { req ->
            count++
            assertNull(req.url.queryParameter("pageToken"))
            200 to "{\"files\":[{\"id\":\"op1\",\"name\":\"op1.json\",\"mimeType\":\"application/json\"}],\"nextPageToken\":\"page2\"}"
        }
        assertEquals(1, client.listFilesInFolder("audit-folder").size)
        assertEquals(1, count)
        println("CONFIRMED: Drive nextPageToken is ignored; only first response is returned")
    }
    @Test fun driveMissingTokenUploadReturnsNullWithoutFailure() = runBlocking {
        val client = DriveSyncClient { null }
        intercept(client) { fail("No request should be attempted"); 500 to "{}" }
        assertNull(client.uploadFile("audit", "audit.json", "{}"))
        println("CONFIRMED: missing-token upload returns null; callers must not mark uploaded")
    }
}
