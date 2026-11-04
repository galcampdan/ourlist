package com.ourlist.app.data.sync

import com.ourlist.app.data.alarm.ReminderScheduler
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
    private val driveClient: DriveSyncClient
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
                val folderId = dataStoreManager.sharedFolderIdFlow.first()
                if (folderId.isEmpty()) {
                    _syncState.value = SyncState.Idle
                    return@withContext SyncState.Idle
                }

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
                val result = SyncState.Success(
                    syncedCount = processedRemoteCount + pending.size,
                    conflictCount = unresolvedConflicts
                )
                _syncState.value = result
                result
            } catch (e: Exception) {
                val error = SyncState.Error(e.message ?: "동기화 중 오류가 발생했습니다.")
                _syncState.value = error
                error
            }
        }
    }

    private suspend fun processRemoteOperations(
        folderId: String,
        myDeviceId: String,
        mySub: String
    ): Int {
        val files = driveClient.listFilesInFolder(folderId)
        var appliedCount = 0

        for (file in files) {
            if (!file.name.endsWith(".json")) continue
            val opId = file.name.removeSuffix(".json")
            if (syncDao.exists(opId)) continue

            val content = driveClient.downloadFileContent(file.id) ?: continue
            val remoteOp = try {
                json.decodeFromString<SyncOperation>(content)
            } catch (e: Exception) {
                continue
            }

            // Skip own operations
            if (remoteOp.deviceId == myDeviceId || (mySub.isNotEmpty() && remoteOp.actorSub == mySub)) {
                syncDao.insert(SyncOperationEntity.fromDomain(remoteOp, isUploaded = true))
                continue
            }

            applyRemoteOperation(remoteOp)
            syncDao.insert(SyncOperationEntity.fromDomain(remoteOp, isUploaded = true))
            appliedCount++
        }
        return appliedCount
    }

    private suspend fun applyRemoteOperation(op: SyncOperation) {
        val localEntity = bucketDao.getItemById(op.itemId)
        val localItem = localEntity?.toDomain()

        when (op.kind) {
            OpKind.CREATE -> {
                if (localItem == null) {
                    val snapshot = op.itemSnapshotJson?.let {
                        try { json.decodeFromString<BucketItem>(it) } catch (e: Exception) { null }
                    }
                    if (snapshot != null) {
                        bucketDao.upsert(BucketItemEntity.fromDomain(snapshot))
                        notificationRepository.recordPartnerAction(
                            actorName = op.actorName,
                            actionType = "CREATE",
                            itemId = snapshot.id,
                            itemTitle = snapshot.title,
                            collection = snapshot.collection
                        )
                        val isReminderOn = dataStoreManager.isReminderEnabled(snapshot.id)
                        reminderScheduler.scheduleRemindersForItem(snapshot, isReminderOn)
                    }
                }
            }

            OpKind.UPDATE_FIELD -> {
                if (localItem != null) {
                    val remoteSnapshot = op.itemSnapshotJson?.let {
                        try { json.decodeFromString<BucketItem>(it) } catch (e: Exception) { null }
                    }

                    if (remoteSnapshot != null) {
                        // Check for field-level concurrency conflict
                        val hasLocalEdit = localItem.updatedAt > op.createdAt
                        if (hasLocalEdit && localItem.title != remoteSnapshot.title && op.fieldPath == "title") {
                            // Conflict detected on title
                            conflictDao.insert(
                                ConflictEntity(
                                    conflictId = UUID.randomUUID().toString(),
                                    itemId = localItem.id,
                                    itemTitle = localItem.title,
                                    collection = localItem.collection.name,
                                    conflictType = ConflictType.FIELD_CONFLICT.name,
                                    fieldPath = "title",
                                    localValue = localItem.title,
                                    remoteValue = remoteSnapshot.title,
                                    remoteActorName = op.actorName,
                                    remoteUpdatedAt = op.createdAt,
                                    createdAt = System.currentTimeMillis(),
                                    isResolved = false
                                )
                            )
                        } else {
                            // Auto-merge non-conflicting fields
                            val mergedItem = localItem.copy(
                                title = if (!hasLocalEdit) remoteSnapshot.title else localItem.title,
                                targetDate = if (!hasLocalEdit || localItem.targetDate == null) remoteSnapshot.targetDate else localItem.targetDate,
                                placeText = if (!hasLocalEdit || localItem.placeText.isNullOrEmpty()) remoteSnapshot.placeText else localItem.placeText,
                                memo = if (!hasLocalEdit || localItem.memo.isNullOrEmpty()) remoteSnapshot.memo else localItem.memo,
                                estimatedCostKRW = if (!hasLocalEdit || localItem.estimatedCostKRW == null) remoteSnapshot.estimatedCostKRW else localItem.estimatedCostKRW,
                                wishType = if (!hasLocalEdit) remoteSnapshot.wishType else localItem.wishType,
                                updatedAt = System.currentTimeMillis()
                            )
                            bucketDao.upsert(BucketItemEntity.fromDomain(mergedItem))
                            val isReminderOn = dataStoreManager.isReminderEnabled(mergedItem.id)
                            reminderScheduler.scheduleRemindersForItem(mergedItem, isReminderOn)
                        }
                    }

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
                // Auto-merged
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
                    val hasLocalRecentEdit = localItem.updatedAt > op.createdAt
                    if (hasLocalRecentEdit && localItem.deletedAt == null) {
                        // Delete vs Update Conflict!
                        conflictDao.insert(
                            ConflictEntity(
                                conflictId = UUID.randomUUID().toString(),
                                itemId = localItem.id,
                                itemTitle = localItem.title,
                                collection = localItem.collection.name,
                                conflictType = ConflictType.DELETE_VS_UPDATE.name,
                                fieldPath = null,
                                localValue = "내 수정본 유지",
                                remoteValue = "상대방의 삭제 수용",
                                remoteActorName = op.actorName,
                                remoteUpdatedAt = op.createdAt,
                                createdAt = System.currentTimeMillis(),
                                isResolved = false
                            )
                        )
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
