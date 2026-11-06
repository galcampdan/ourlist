package com.ourlist.app.data.backup

import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.local.entity.BucketItemEntity
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.sync.DriveSyncClient
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RestoreMode {
    OVERWRITE,
    MERGE
}

class BackupManager(
    private val db: OurListDatabase,
    private val dataStoreManager: DataStoreManager,
    private val driveClient: DriveSyncClient
) {
    private val bucketDao = db.bucketDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createEncryptedBackup(password: String): String = withContext(Dispatchers.IO) {
        val personalItems = bucketDao.getAllInCollection(BucketCollection.PERSONAL.name).map { it.toDomain() }
        val secretItems = bucketDao.getAllInCollection(BucketCollection.SECRET_LOCAL.name).map { it.toDomain() }
        val allPersonal = personalItems + secretItems

        val plainJson = json.encodeToString(allPersonal)
        var recoveryCode = dataStoreManager.recoveryCodeFlow.first()
        if (recoveryCode.isBlank()) {
            recoveryCode = CryptoEngine.generateRecoveryCode()
            dataStoreManager.setRecoveryCode(recoveryCode)
        }
        CryptoEngine.encryptBackup(plainJson, password, recoveryCode)
    }

    suspend fun restoreEncryptedBackup(
        encryptedBase64: String,
        passOrRecoveryCode: String,
        mode: RestoreMode
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val decryptedJson = CryptoEngine.decryptBackup(encryptedBase64, passOrRecoveryCode)
            val items = json.decodeFromString<List<BucketItem>>(decryptedJson)

            require(items.all { it.collection in setOf(BucketCollection.PERSONAL, BucketCollection.SECRET_LOCAL) }) { "개인 백업에 공유 데이터가 포함되어 있습니다." }
            require(items.map { it.id }.distinct().size == items.size) { "백업의 항목 ID가 중복됩니다." }
            db.withTransaction {
            for (item in items) {
                val existing = bucketDao.getItemById(item.id)
                require(existing == null || existing.collection in setOf(BucketCollection.PERSONAL.name, BucketCollection.SECRET_LOCAL.name)) { "공유 항목과 백업 ID가 충돌합니다." }
            }
            if (mode == RestoreMode.OVERWRITE) {
                // Remove existing personal and secret local items
                bucketDao.deleteCollections(listOf(BucketCollection.PERSONAL.name, BucketCollection.SECRET_LOCAL.name))
                bucketDao.upsertAll(items.map { BucketItemEntity.fromDomain(it) })
            } else {
                // Merge mode
                for (item in items) {
                    val existing = bucketDao.getItemById(item.id)
                    if (existing == null || item.updatedAt > existing.updatedAt) {
                        bucketDao.upsert(BucketItemEntity.fromDomain(item))
                    }
                }
            }
            }
            Result.success(items.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listAutoBackups(): List<com.ourlist.app.data.sync.DriveFileMetadata> =
        driveClient.listFilesInFolder("appDataFolder").filter { it.name.startsWith("ourlist_backup_") && it.name.endsWith(".enc") }.sortedByDescending { it.name }

    suspend fun readAutoBackup(id: String): String {
        require(listAutoBackups().any { it.id == id }) { "선택한 자동 백업을 찾을 수 없습니다." }
        return driveClient.downloadFileContent(id)
    }

    suspend fun performAutoDriveBackup(password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encrypted = createEncryptedBackup(password)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "ourlist_backup_${sdf.format(Date())}.enc"

            // Upload to Google Drive appDataFolder (private to this app)
            driveClient.uploadFile("appDataFolder", fileName, encrypted, "application/octet-stream")

            // Rotate: keep only the latest 7 backups
            val appDataFiles = driveClient.listFilesInFolder("appDataFolder")
            val backupFiles = appDataFiles.filter { it.name.startsWith("ourlist_backup_") && it.name.endsWith(".enc") }
                .sortedByDescending { it.name }

            for (old in backupFiles.drop(7)) driveClient.deleteFile(old.id)

            dataStoreManager.setLastBackupTime(System.currentTimeMillis())
            dataStoreManager.setLastBackupError("")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dataStoreManager.setLastBackupError(e.message ?: "자동 백업에 실패했습니다.")
            false
        }
    }
}

object ExportManager {
    fun selectItems(items: List<BucketItem>, collections: Set<BucketCollection>, includeTrash: Boolean = false): List<BucketItem> =
        items.filter { it.collection in collections && (includeTrash || !it.isDeleted) }


    fun exportToJson(items: List<BucketItem>): String {
        val json = Json { prettyPrint = true }
        return json.encodeToString(items)
    }

    fun exportToMarkdown(title: String, items: List<BucketItem>): String {
        val sb = StringBuilder()
        sb.appendLine("# Our List 버킷리스트 — $title")
        sb.appendLine()
        sb.appendLine("> 생성일시: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date())}")
        sb.appendLine()

        for (item in items) {
            val statusIcon = if (item.isDone) "[x]" else "[ ]"
            sb.appendLine("### $statusIcon ${item.title}")
            sb.appendLine("- **영역**: ${item.collection.displayName}")
            sb.appendLine("- **유형**: ${item.wishType.label}")
            if (!item.targetDate.isNullOrEmpty()) {
                sb.appendLine("- **목표 날짜**: ${item.targetDate}")
            }
            if (item.estimatedCostKRW != null && item.estimatedCostKRW > 0) {
                sb.appendLine("- **예상 비용**: %,d원".format(item.estimatedCostKRW))
            }
            if (!item.placeText.isNullOrEmpty()) {
                sb.appendLine("- **장소**: ${item.placeText}")
            }
            if (!item.memo.isNullOrEmpty()) {
                sb.appendLine("- **메모**: ${item.memo}")
            }
            if (item.checklist.isNotEmpty()) {
                sb.appendLine("- **준비물 체크리스트**:")
                for (check in item.checklist) {
                    val box = if (check.checked) "[x]" else "[ ]"
                    sb.appendLine("  - $box ${check.text}")
                }
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    fun exportToCsv(items: List<BucketItem>): String {
        val sb = StringBuilder()
        sb.appendLine("ID,영역,제목,희망유형,목표날짜,예상비용(KRW),장소,메모,상태,완료일시,체크리스트")
        for (item in items) {
            val checklistStr = item.checklist.joinToString("; ") { "${if (it.checked) "V" else "X"} ${it.text}" }
            sb.appendLine(
                listOf(
                    escapeCsv(item.id),
                    escapeCsv(item.collection.displayName),
                    escapeCsv(item.title),
                    escapeCsv(item.wishType.label),
                    escapeCsv(item.targetDate ?: ""),
                    escapeCsv(item.estimatedCostKRW?.toString() ?: ""),
                    escapeCsv(item.placeText ?: ""),
                    escapeCsv(item.memo ?: ""),
                    escapeCsv(if (item.isDone) "완료" else "미완료"),
                    escapeCsv(item.completedAt?.toString() ?: ""),
                    escapeCsv(checklistStr)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    private fun escapeCsv(text: String): String {
        val safe = if (text.trimStart().firstOrNull() in listOf('=', '+', '-', '@')) "'" + text else text
        val escaped = safe.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
