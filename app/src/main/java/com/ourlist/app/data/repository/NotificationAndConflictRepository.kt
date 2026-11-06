package com.ourlist.app.data.repository

import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.local.entity.BucketItemEntity
import com.ourlist.app.data.local.entity.ConflictEntity
import com.ourlist.app.data.local.entity.InAppNotificationEntity
import com.ourlist.app.data.local.entity.SyncOperationEntity
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.ConflictRecord
import com.ourlist.app.data.model.ConflictType
import com.ourlist.app.data.model.InAppNotification
import com.ourlist.app.data.model.OpKind
import com.ourlist.app.data.model.SyncOperation
import com.ourlist.app.data.model.WishType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class NotificationRepository(private val db: OurListDatabase) {
    private val dao = db.inAppNotificationDao()

    fun getNotifications(): Flow<List<InAppNotification>> {
        return dao.getAllNotificationsFlow().map { list -> list.map { it.toDomain() } }
    }

    fun getUnreadCount(): Flow<Int> = dao.getUnreadCountFlow()

    suspend fun markAsRead(id: String) = dao.markAsRead(id)

    suspend fun markAllAsRead() = dao.markAllAsRead()

    suspend fun delete(id: String) = dao.delete(id)

    suspend fun recordPartnerAction(
        actorName: String,
        actionType: String, // CREATE, UPDATE, COMPLETE, DELETE, RESTORE
        itemId: String,
        itemTitle: String,
        collection: BucketCollection
    ) {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 60 * 60 * 1000L

        // Check if there was an edit notification for this same item in the last hour to aggregate
        if (actionType == "UPDATE") {
            val recent = dao.findRecentForSameItem(itemId, oneHourAgo)
            if (recent != null && recent.actionType == "UPDATE") {
                val newCount = recent.editCount + 1
                val updated = recent.copy(
                    editCount = newCount,
                    message = "${actorName}님이 '$itemTitle' 항목을 ${newCount}회 수정했습니다.",
                    timestamp = now,
                    isRead = false
                )
                dao.update(updated)
                return
            }
        }

        val message = when (actionType) {
            "CREATE" -> "${actorName}님이 새 버킷리스트 '$itemTitle'을(를) 등록했습니다."
            "UPDATE" -> "${actorName}님이 '$itemTitle' 항목을 수정했습니다."
            "COMPLETE" -> "🎉 ${actorName}님이 '$itemTitle'을(를) 완료했습니다!"
            "DELETE" -> "${actorName}님이 '$itemTitle'을(를) 삭제(휴지통)했습니다."
            "RESTORE" -> "${actorName}님이 '$itemTitle'을(를) 복원했습니다."
            else -> "${actorName}님이 '$itemTitle' 항목을 변경했습니다."
        }

        val notification = InAppNotification(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            itemTitle = itemTitle,
            collection = collection,
            actorName = actorName,
            actionType = actionType,
            message = message,
            editCount = 1,
            timestamp = now,
            isRead = false
        )
        dao.insert(InAppNotificationEntity.fromDomain(notification))
    }
}

class ConflictRepository(
    private val db: OurListDatabase,
    private val dataStoreManager: DataStoreManager
) {
    private val conflictDao = db.conflictDao()
    private val bucketDao = db.bucketDao()
    private val syncDao = db.syncOperationDao()
    private val json = Json { ignoreUnknownKeys = true }

    fun getUnresolvedConflicts(): Flow<List<ConflictRecord>> {
        return conflictDao.getUnresolvedConflictsFlow().map { list -> list.map { it.toDomain() } }
    }

    fun getUnresolvedCount(): Flow<Int> = conflictDao.getUnresolvedCountFlow()

    suspend fun resolveFieldConflict(
        conflict: ConflictRecord,
        chosenValue: String?
    ) {
        val itemEntity = bucketDao.getItemById(conflict.itemId) ?: return
        val item = itemEntity.toDomain()
        val now = System.currentTimeMillis()

        // Apply chosen value to the specific field
        val updatedItem = when (conflict.fieldPath) {
            "title" -> item.copy(title = chosenValue ?: item.title, updatedAt = now)
            "targetDate" -> item.copy(targetDate = chosenValue, updatedAt = now)
            "placeText" -> item.copy(placeText = chosenValue, updatedAt = now)
            "memo" -> item.copy(memo = chosenValue, updatedAt = now)
            "estimatedCostKRW" -> item.copy(estimatedCostKRW = chosenValue?.toLongOrNull(), updatedAt = now)
            "checklist" -> item.copy(checklist = chosenValue?.let { json.decodeFromString<List<com.ourlist.app.data.model.ChecklistItem>>(it) } ?: emptyList(), updatedAt = now)
            "completedAt" -> item.copy(completedAt = chosenValue?.toLongOrNull(), updatedAt = now)
            "wishType" -> item.copy(wishType = chosenValue?.let { WishType.valueOf(it) } ?: item.wishType, updatedAt = now)
            else -> item.copy(updatedAt = now)
        }
        bucketDao.upsert(BucketItemEntity.fromDomain(updatedItem))
        conflictDao.resolveConflict(conflict.conflictId)

        // Propagate resolution via SyncOperation
        val deviceId = dataStoreManager.getOrCreateDeviceId()
        val userSub = dataStoreManager.userSubFlow.first()
        val userName = dataStoreManager.userNameFlow.first()
        val seq = dataStoreManager.getNextDeviceSequence()

        val op = SyncOperation(
            opId = UUID.randomUUID().toString(),
            collection = conflict.collection,
            deviceId = deviceId,
            actorSub = userSub,
            actorName = userName,
            itemId = conflict.itemId,
            kind = OpKind.UPDATE_FIELD,
            fieldPath = conflict.fieldPath,
            fieldValueJson = chosenValue,
            resolvesConflict = true,
            itemSnapshotJson = json.encodeToString(updatedItem),
            createdAt = now,
            deviceSequence = seq
        )
        syncDao.insert(SyncOperationEntity.fromDomain(op))
    }

    suspend fun resolveDeleteVsUpdateConflict(
        conflict: ConflictRecord,
        keepItem: Boolean
    ) {
        val now = System.currentTimeMillis()
        val deviceId = dataStoreManager.getOrCreateDeviceId()
        val userSub = dataStoreManager.userSubFlow.first()
        val userName = dataStoreManager.userNameFlow.first()
        val seq = dataStoreManager.getNextDeviceSequence()

        if (keepItem) {
            // Restore and keep item
            val itemEntity = bucketDao.getItemById(conflict.itemId)
            if (itemEntity != null) {
                val restored = itemEntity.toDomain().copy(deletedAt = null, updatedAt = now)
                bucketDao.upsert(BucketItemEntity.fromDomain(restored))
                val op = SyncOperation(
                    opId = UUID.randomUUID().toString(),
                    collection = conflict.collection,
                    deviceId = deviceId,
                    actorSub = userSub,
                    actorName = userName,
                    itemId = conflict.itemId,
                    kind = OpKind.RESTORE,
                    createdAt = now,
                    deviceSequence = seq
                )
                syncDao.insert(SyncOperationEntity.fromDomain(op))
            }
        } else {
            // Keep deleted
            val itemEntity = bucketDao.getItemById(conflict.itemId)
            if (itemEntity != null) {
                val deleted = itemEntity.toDomain().copy(deletedAt = now, updatedAt = now)
                bucketDao.upsert(BucketItemEntity.fromDomain(deleted))
                val op = SyncOperation(
                    opId = UUID.randomUUID().toString(),
                    collection = conflict.collection,
                    deviceId = deviceId,
                    actorSub = userSub,
                    actorName = userName,
                    itemId = conflict.itemId,
                    kind = OpKind.DELETE,
                    fieldValueJson = now.toString(),
                    createdAt = now,
                    deviceSequence = seq
                )
                syncDao.insert(SyncOperationEntity.fromDomain(op))
            }
        }
        conflictDao.resolveConflict(conflict.conflictId)
    }
}
