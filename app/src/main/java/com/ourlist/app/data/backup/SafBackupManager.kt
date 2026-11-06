package com.ourlist.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.model.BucketCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable
data class SafFullBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val shared: List<com.ourlist.app.data.model.BucketItem> = emptyList(),
    val heart: List<com.ourlist.app.data.model.BucketItem> = emptyList()
)

class SafBackupManager(
    private val context: Context,
    private val db: OurListDatabase,
    private val dataStoreManager: DataStoreManager
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun backupNow(explicitUriStr: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val uriStr = explicitUriStr ?: dataStoreManager.safBackupTreeUriFlow.first()
            if (uriStr.isBlank()) {
                return@withContext Result.failure(IllegalStateException("백업 폴더가 지정되지 않았습니다."))
            }

            val isEnabled = dataStoreManager.safBackupEnabledFlow.first()
            if (!isEnabled && explicitUriStr == null) {
                return@withContext Result.success(0)
            }

            val treeUri = Uri.parse(uriStr)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(IOException("지정된 폴더에 접근할 수 없습니다: $uriStr"))

            if (!rootDoc.canWrite()) {
                return@withContext Result.failure(IOException("지정된 폴더에 쓰기 권한이 없습니다."))
            }

            val bucketDao = db.bucketDao()
            val sharedItems = bucketDao.getAllInCollection(BucketCollection.SHARED.name).map { it.toDomain() }
            val heartItems = bucketDao.getAllInCollection(BucketCollection.HEART.name).map { it.toDomain() }

            // 1. Write bucketlist.json (Shared bucket list)
            val sharedJson = json.encodeToString(sharedItems)
            writeDocumentFile(rootDoc, "bucketlist.json", "application/json", sharedJson)

            // 2. Write heart.json (Couple heart list)
            val heartJson = json.encodeToString(heartItems)
            writeDocumentFile(rootDoc, "heart.json", "application/json", heartJson)

            // 3. Write ourlist_backup_full.json (Comprehensive snapshot)
            val fullBackup = SafFullBackup(
                exportedAt = System.currentTimeMillis(),
                shared = sharedItems,
                heart = heartItems
            )
            val fullJson = json.encodeToString(fullBackup)
            writeDocumentFile(rootDoc, "ourlist_backup_full.json", "application/json", fullJson)

            val now = System.currentTimeMillis()
            dataStoreManager.setLastSafBackupTime(now)
            dataStoreManager.setLastSafBackupError("")

            val totalCount = sharedItems.size + heartItems.size
            Result.success(totalCount)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            dataStoreManager.setLastSafBackupError(e.message ?: "폴더 백업에 실패했습니다.")
            Result.failure(e)
        }
    }

    private fun writeDocumentFile(
        parent: DocumentFile,
        fileName: String,
        mimeType: String,
        content: String
    ) {
        var targetFile = parent.findFile(fileName)
        if (targetFile == null) {
            targetFile = parent.createFile(mimeType, fileName)
                ?: throw IOException("파일 생성 실패: $fileName")
        }

        context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: throw IOException("파일 쓰기 스트림을 열 수 없습니다: $fileName")
    }

    fun getFriendlyFolderName(uriStr: String): String {
        if (uriStr.isBlank()) return "선택 안 됨"
        return try {
            val treeUri = Uri.parse(uriStr)
            val doc = DocumentFile.fromTreeUri(context, treeUri)
            val name = doc?.name
            if (!name.isNullOrBlank()) {
                name
            } else {
                val decoded = Uri.decode(treeUri.lastPathSegment ?: "")
                decoded.substringAfterLast(":").ifBlank { "Google Drive 폴더" }
            }
        } catch (e: Exception) {
            "Google Drive 백업 폴더"
        }
    }
}
