package com.ourlist.app.data.sync

import com.ourlist.app.data.alarm.ReminderScheduler
import com.ourlist.app.data.backup.SafBackupManager
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.local.entity.BucketItemEntity
import com.ourlist.app.data.local.entity.ConflictEntity
import com.ourlist.app.data.local.entity.SyncOperationEntity
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.ConflictType
import com.ourlist.app.data.model.OpKind
import com.ourlist.app.data.model.SyncOperation
import com.ourlist.app.data.repository.NotificationRepository
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val syncedCount: Int, val conflictCount: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}

class SyncEngine(
    private val db: OurListDatabase,
    private val dataStoreManager: DataStoreManager,
    private val notificationRepository: NotificationRepository,
    private val reminderScheduler: ReminderScheduler,
    private val driveClient: DriveSyncClient,
    private val bucketRepository: com.ourlist.app.data.repository.BucketRepository,
    private val coupleClient: CoupleSyncClient? = null,
    private val safBackupManager: SafBackupManager? = null
) {
    private val bucketDao = db.bucketDao()
    private val syncDao = db.syncOperationDao()
    private val conflictDao = db.conflictDao()
    private val json = Json { ignoreUnknownKeys = true }
    private val syncMutex = Mutex()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun sync(): SyncState = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            _syncState.value = SyncState.Syncing
            try {
                val coupleCode = dataStoreManager.coupleCodeFlow.first().trim()
                if (coupleCode.isNotEmpty() && dataStoreManager.coupleProtocolFlow.first() < 2) {
                    throw java.io.IOException("보안 연동을 위해 더보기에서 새 코드로 다시 연결해주세요. 기존 목록은 유지됩니다.")
                }
                if (coupleCode.isNotEmpty() && coupleClient != null) {
                    val result = syncViaCoupleCode(coupleCode)
                    if (result is SyncState.Success && result.syncedCount > 0) {
                        try { safBackupManager?.backupNow() } catch (e: Exception) { /* non-fatal */ }
                    }
                    _syncState.value = result
                    return@withContext result
                }

                val folderId = dataStoreManager.sharedFolderIdFlow.first().trim()
                if (folderId.isNotEmpty()) {
                    val result = syncViaDrive(folderId)
                    if (result is SyncState.Success && result.syncedCount > 0) {
                        try { safBackupManager?.backupNow() } catch (e: Exception) { /* non-fatal */ }
                    }
                    _syncState.value = result
                    return@withContext result
                }

                _syncState.value = SyncState.Idle
                SyncState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = SyncState.Error(e.message ?: "동기화 중 오류가 발생했습니다.")
                _syncState.value = error
                error
            }
        }
    }

    private suspend fun syncViaCoupleCode(coupleCode: String): SyncState {
        val client = coupleClient ?: return SyncState.Idle
        val myDeviceId = dataStoreManager.getOrCreateDeviceId()
        val mySub = dataStoreManager.userSubFlow.first()

        // Verify partner connection state via room metadata
        val meta = client.fetchMeta(coupleCode) ?: throw java.io.IOException("연결된 커플 방을 찾을 수 없습니다.")
        require(meta.isMember(myDeviceId)) { "이 기기는 연결된 방의 구성원이 아닙니다." }
        if (meta.isBothConnected) {
            if (!dataStoreManager.isPartnerConnectedFlow.first()) {
                val initial = client.fetchState(coupleCode) ?: throw java.io.IOException("공유 목록을 받지 못했습니다.")
                bucketRepository.importSharedSnapshot(initial)
            }
            dataStoreManager.setPartnerConnected(true)
        } else {
            dataStoreManager.setPartnerConnected(false)
        }

        if (meta != null) {
            val localDatingDate = dataStoreManager.datingStartDateFlow.first()
            if (meta.datingStartDate.isNotBlank() && meta.datingStartDate != localDatingDate) {
                dataStoreManager.setDatingStartDate(meta.datingStartDate)
            } else if (meta.datingStartDate.isBlank() && localDatingDate.isNotBlank()) {
                client.updateDatingStartDate(coupleCode, localDatingDate)
            }
        }

        var processedRemoteCount = 0

        // Step 1: Download & Apply Remote Operations
        val remoteOps = client.fetchOperations(coupleCode)
        for (remoteOp in orderedOperations(remoteOps)) {
            if (syncDao.exists(remoteOp.opId)) continue

            // Skip own operations
            if (remoteOp.deviceId == myDeviceId) {
                syncDao.insert(SyncOperationEntity.fromDomain(remoteOp, isUploaded = true))
                continue
            }

            db.withTransaction {
                applyRemoteOperation(remoteOp)
                syncDao.insert(SyncOperationEntity.fromDomain(remoteOp, isUploaded = true))
            }
            processedRemoteCount++
        }

        // Step 2: Upload Local Pending Operations
        val pending = syncDao.getPendingOperations()
        for (entity in pending) {
            val op = entity.toDomain()
            val ok = client.pushOperation(coupleCode, op)
            if (!ok) throw java.io.IOException("변경사항 전송에 실패했습니다. 다시 동기화해주세요.")
            syncDao.markUploaded(op.opId)
        }

        // Step 3: Check cold start snapshot recovery
        val localShared = bucketDao.getAllInCollection(BucketCollection.SHARED.name)
        val localHeart = bucketDao.getAllInCollection(BucketCollection.HEART.name)
        if (localShared.isEmpty() && localHeart.isEmpty() && processedRemoteCount == 0 && pending.isEmpty()) {
            val remoteState = client.fetchState(coupleCode)
            if (remoteState != null) {
                for (item in remoteState.shared) {
                    bucketDao.upsert(BucketItemEntity.fromDomain(item))
                }
                for (item in remoteState.heart) {
                    bucketDao.upsert(BucketItemEntity.fromDomain(item))
                }
                processedRemoteCount += remoteState.shared.size + remoteState.heart.size
            }
        }

        // Step 4: Update Snapshots for partner cold start
        val snapshot = db.withTransaction {
            RoomStateData(
                shared = bucketDao.getAllInCollection(BucketCollection.SHARED.name).map { it.toDomain() },
                heart = bucketDao.getAllInCollection(BucketCollection.HEART.name).map { it.toDomain() },
                appliedOperationIds = syncDao.getAllOperationIds()
            )
        }
        if (!client.pushState(coupleCode, myDeviceId, snapshot.shared, snapshot.heart, snapshot.appliedOperationIds)) throw java.io.IOException("공유 상태 저장에 실패했습니다.")

        // Step 5: Housekeeping - Purge trash older than 30 days
        bucketDao.purgeTrashOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L)

        val now = System.currentTimeMillis()
        dataStoreManager.setLastSyncTime(now)

        val unresolvedConflicts = conflictDao.getUnresolvedConflicts().size
        return SyncState.Success(
            syncedCount = processedRemoteCount + pending.size,
            conflictCount = unresolvedConflicts
        )
    }

    private suspend fun syncViaDrive(folderId: String): SyncState {
        val myDeviceId = dataStoreManager.getOrCreateDeviceId()
        val mySub = dataStoreManager.userSubFlow.first()

        // Step 1: Ensure directory structure in Drive
        val rootFiles = driveClient.listFilesInFolder(folderId)
        var opsFolderId = rootFiles.firstOrNull { it.name == "operations" && it.mimeType == "application/vnd.google-apps.folder" }?.id
        if (opsFolderId == null) {
            opsFolderId = driveClient.createFolder("operations", folderId)
        }

        var sharedOpsFolderId: String? = null
        var heartOpsFolderId: String? = null
        if (opsFolderId != null) {
            val opFolders = driveClient.listFilesInFolder(opsFolderId)
            sharedOpsFolderId = opFolders.firstOrNull { it.name == "shared" }?.id
                ?: driveClient.createFolder("shared", opsFolderId)
            heartOpsFolderId = opFolders.firstOrNull { it.name == "heart" }?.id
                ?: driveClient.createFolder("heart", opsFolderId)
        }
        check(opsFolderId != null && sharedOpsFolderId != null && heartOpsFolderId != null) { "동기화 폴더를 준비하지 못했습니다." }

        var processedRemoteCount = 0

        // Step 2: Download & Apply Remote Operations for SHARED & HEART
        if (sharedOpsFolderId != null) {
            processedRemoteCount += processRemoteOperations(sharedOpsFolderId, myDeviceId, mySub)
        }
        if (heartOpsFolderId != null) {
            processedRemoteCount += processRemoteOperations(heartOpsFolderId, myDeviceId, mySub)
        }

        // Step 3: Upload Local Pending Operations
        val pending = syncDao.getPendingOperations()
        for (entity in pending) {
            val targetFolderId = if (entity.collection == BucketCollection.SHARED.name) {
                sharedOpsFolderId
            } else {
                heartOpsFolderId
            }
            if (targetFolderId != null) {
                val op = entity.toDomain()
                val fileName = "${op.opId}.json"
                val content = json.encodeToString(op)
                driveClient.uploadFile(targetFolderId, fileName, content)
                syncDao.markUploaded(op.opId)
            }
        }

        // Step 4: Update Snapshots for fast cold recovery
        updateRemoteSnapshots(folderId)

        // Step 5: Housekeeping - Purge trash older than 30 days
        bucketDao.purgeTrashOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L)

        val now = System.currentTimeMillis()
        dataStoreManager.setLastSyncTime(now)

        val unresolvedConflicts = conflictDao.getUnresolvedConflicts().size
        return SyncState.Success(
            syncedCount = processedRemoteCount + pending.size,
            conflictCount = unresolvedConflicts
        )
    }

    private suspend fun processRemoteOperations(
        folderId: String,
        myDeviceId: String,
        mySub: String
    ): Int {
        val files = driveClient.listFilesInFolder(folderId)
        var appliedCount = 0

        val operations = mutableListOf<SyncOperation>()
        for (file in files) {
            if (!file.name.endsWith(".json")) continue
            val opId = file.name.removeSuffix(".json")
            if (syncDao.exists(opId)) continue

            val content = driveClient.downloadFileContent(file.id)
            val remoteOp = try {
                json.decodeFromString<SyncOperation>(content)
            } catch (e: Exception) {
                throw java.io.IOException("동기화 파일을 읽을 수 없습니다: ${file.name}", e)
            }
            require(remoteOp.opId == opId) { "동기화 파일 ID가 올바르지 않습니다." }
            operations.add(remoteOp)
        }
        for (remoteOp in orderedOperations(operations)) {
            // Skip own operations
            if (remoteOp.deviceId == myDeviceId) {
                syncDao.insert(SyncOperationEntity.fromDomain(remoteOp, isUploaded = true))
                continue
            }

            db.withTransaction {
                applyRemoteOperation(remoteOp)
                syncDao.insert(SyncOperationEntity.fromDomain(remoteOp, isUploaded = true))
            }
            appliedCount++
        }
        return appliedCount
    }

    private suspend fun applyRemoteOperation(op: SyncOperation) {
        require(op.collection in setOf(BucketCollection.SHARED, BucketCollection.HEART)) { "공유할 수 없는 목록의 변경사항입니다." }
        val localEntity = bucketDao.getItemById(op.itemId)
        val localItem = localEntity?.toDomain()

        // Check pending local operations for this item to determine true causal concurrency
        val pendingOps = syncDao.getPendingOpsForItem(op.itemId)
        val hasPendingLocalOps = pendingOps.isNotEmpty()
        val localPendingDelete = pendingOps.any { it.kind == OpKind.DELETE.name }
        val localPendingUpdate = pendingOps.any { it.kind == OpKind.UPDATE_FIELD.name }
        val locallyModifiedFields = pendingOps
            .filter { it.kind == OpKind.UPDATE_FIELD.name }
            .mapNotNull { it.fieldPath }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .toSet()

        when (op.kind) {
            OpKind.CREATE -> {
                val created = createOrMove(localItem, op)
                if (created != null) {
                    bucketDao.upsert(BucketItemEntity.fromDomain(created))
                    notificationRepository.recordPartnerAction(op.actorName, "CREATE", created.id, created.title, created.collection)
                    reminderScheduler.scheduleRemindersForItem(created, dataStoreManager.isReminderEnabled(created.id))
                }
            }

            OpKind.UPDATE_FIELD -> {
                if (localItem != null) {
                    if (localPendingDelete) {
                        // Conflict: local deleted, remote updated
                        createDeleteVsUpdateConflict(localItem, "내 삭제 유지", "상대방 수정본 복원", op)
                        return
                    }

                    val merged = FieldMerge.merge(localItem, op, locallyModifiedFields)
                    for (difference in merged.conflicts) {
                        createFieldConflict(localItem, difference.path, difference.local, difference.remote, op)
                    }
                    bucketDao.upsert(BucketItemEntity.fromDomain(merged.item))
                    val isReminderOn = dataStoreManager.isReminderEnabled(merged.item.id)
                    reminderScheduler.scheduleRemindersForItem(merged.item, isReminderOn)

                    notificationRepository.recordPartnerAction(
                        actorName = op.actorName,
                        actionType = "UPDATE",
                        itemId = localItem.id,
                        itemTitle = localItem.title,
                        collection = localItem.collection
                    )
                }
            }

            OpKind.CHECKLIST_TOGGLE -> {
                if (localItem != null) {
                    val checkId = op.fieldPath
                    val checked = op.fieldValueJson?.toBooleanStrictOrNull() ?: false
                    val updatedChecklist = localItem.checklist.map {
                        if (it.id == checkId) it.copy(checked = checked) else it
                    }
                    val updated = localItem.copy(checklist = updatedChecklist, updatedAt = System.currentTimeMillis())
                    bucketDao.upsert(BucketItemEntity.fromDomain(updated))
                }
            }

            OpKind.CHECKLIST_EDIT -> {
                // Merged via snapshot checklist merge
            }

            OpKind.COMPLETE -> {
                if (localItem != null) {
                    val completedAt = op.fieldValueJson?.toLongOrNull() ?: op.createdAt
                    val updated = localItem.copy(
                        status = BucketStatus.DONE,
                        completedAt = completedAt,
                        updatedAt = System.currentTimeMillis()
                    )
                    bucketDao.upsert(BucketItemEntity.fromDomain(updated))
                    reminderScheduler.cancelRemindersForItem(localItem.id)
                    notificationRepository.recordPartnerAction(
                        actorName = op.actorName,
                        actionType = "COMPLETE",
                        itemId = localItem.id,
                        itemTitle = localItem.title,
                        collection = localItem.collection
                    )
                }
            }

            OpKind.UNCOMPLETE -> {
                if (localItem != null) {
                    val updated = localItem.copy(
                        status = BucketStatus.TODO,
                        completedAt = null,
                        updatedAt = System.currentTimeMillis()
                    )
                    bucketDao.upsert(BucketItemEntity.fromDomain(updated))
                    val isReminderOn = dataStoreManager.isReminderEnabled(localItem.id)
                    reminderScheduler.scheduleRemindersForItem(updated, isReminderOn)
                    notificationRepository.recordPartnerAction(
                        actorName = op.actorName,
                        actionType = "UPDATE",
                        itemId = localItem.id,
                        itemTitle = localItem.title,
                        collection = localItem.collection
                    )
                }
            }

            OpKind.DELETE -> {
                if (localItem != null) {
                    val moveTarget = op.fieldValueJson?.removePrefix("moved_to_")?.let { name -> BucketCollection.entries.firstOrNull { it.name == name } }
                    if (op.moveId != null && moveTarget in setOf(BucketCollection.SHARED, BucketCollection.HEART)) {
                        bucketDao.upsert(BucketItemEntity.fromDomain(localItem.copy(collection = moveTarget!!, deletedAt = null, updatedAt = op.createdAt)))
                    } else if (localPendingUpdate && localItem.deletedAt == null && op.moveId == null) {
                        // Conflict: local updated, remote deleted
                        createDeleteVsUpdateConflict(localItem, "내 수정본 유지", "상대방의 삭제 수용", op)
                    } else {
                        val deletedTime = op.fieldValueJson?.toLongOrNull() ?: op.createdAt
                        val updated = localItem.copy(deletedAt = deletedTime, updatedAt = System.currentTimeMillis())
                        bucketDao.upsert(BucketItemEntity.fromDomain(updated))
                        reminderScheduler.cancelRemindersForItem(localItem.id)
                        notificationRepository.recordPartnerAction(
                            actorName = op.actorName,
                            actionType = "DELETE",
                            itemId = localItem.id,
                            itemTitle = localItem.title,
                            collection = localItem.collection
                        )
                    }
                }
            }

            OpKind.RESTORE -> {
                if (localItem != null) {
                    val updated = localItem.copy(deletedAt = null, updatedAt = System.currentTimeMillis())
                    bucketDao.upsert(BucketItemEntity.fromDomain(updated))
                    val isReminderOn = dataStoreManager.isReminderEnabled(localItem.id)
                    reminderScheduler.scheduleRemindersForItem(updated, isReminderOn)
                    notificationRepository.recordPartnerAction(
                        actorName = op.actorName,
                        actionType = "RESTORE",
                        itemId = localItem.id,
                        itemTitle = localItem.title,
                        collection = localItem.collection
                    )
                }
            }

            OpKind.MOVE -> {
                // Handled idempotently via op
            }
        }
    }

    private suspend fun createFieldConflict(
        localItem: BucketItem,
        fieldPath: String,
        localValue: String?,
        remoteValue: String?,
        op: SyncOperation
    ) {
        conflictDao.insert(
            ConflictEntity(
                conflictId = UUID.randomUUID().toString(),
                itemId = localItem.id,
                itemTitle = localItem.title,
                collection = localItem.collection.name,
                conflictType = ConflictType.FIELD_CONFLICT.name,
                fieldPath = fieldPath,
                localValue = localValue,
                remoteValue = remoteValue,
                remoteActorName = op.actorName,
                remoteUpdatedAt = op.createdAt,
                createdAt = System.currentTimeMillis(),
                isResolved = false
            )
        )
    }

    private suspend fun createDeleteVsUpdateConflict(
        localItem: BucketItem,
        localChoiceText: String,
        remoteChoiceText: String,
        op: SyncOperation
    ) {
        conflictDao.insert(
            ConflictEntity(
                conflictId = UUID.randomUUID().toString(),
                itemId = localItem.id,
                itemTitle = localItem.title,
                collection = localItem.collection.name,
                conflictType = ConflictType.DELETE_VS_UPDATE.name,
                fieldPath = null,
                localValue = localChoiceText,
                remoteValue = remoteChoiceText,
                remoteActorName = op.actorName,
                remoteUpdatedAt = op.createdAt,
                createdAt = System.currentTimeMillis(),
                isResolved = false
            )
        )
    }

    private suspend fun updateRemoteSnapshots(folderId: String) {
        val rootFiles = driveClient.listFilesInFolder(folderId)

        // 1. bucketlist.json
        val sharedItems = bucketDao.getAllInCollection(BucketCollection.SHARED.name).map { it.toDomain() }
        val sharedJson = json.encodeToString(sharedItems)
        val bucketlistFile = rootFiles.firstOrNull { it.name == "bucketlist.json" }
        if (bucketlistFile != null) {
            driveClient.updateFileContent(bucketlistFile.id, sharedJson)
        } else {
            driveClient.uploadFile(folderId, "bucketlist.json", sharedJson)
        }

        // 2. heart.json
        val heartItems = bucketDao.getAllInCollection(BucketCollection.HEART.name).map { it.toDomain() }
        val heartJson = json.encodeToString(heartItems)
        val heartFile = rootFiles.firstOrNull { it.name == "heart.json" }
        if (heartFile != null) {
            driveClient.updateFileContent(heartFile.id, heartJson)
        } else {
            driveClient.uploadFile(folderId, "heart.json", heartJson)
        }
    }
}
