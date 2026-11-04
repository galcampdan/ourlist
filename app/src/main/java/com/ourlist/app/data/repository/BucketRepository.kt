package com.ourlist.app.data.repository

import com.ourlist.app.data.alarm.ReminderScheduler
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.local.entity.BucketItemEntity
import com.ourlist.app.data.local.entity.SyncOperationEntity
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.OpKind
import com.ourlist.app.data.model.SyncOperation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class BucketRepository(
    private val db: OurListDatabase,
    private val dataStoreManager: DataStoreManager,
    private val reminderScheduler: ReminderScheduler
) {
    private val bucketDao = db.bucketDao()
    private val syncDao = db.syncOperationDao()
    private val json = Json { ignoreUnknownKeys = true }

    fun getTodoItems(collection: BucketCollection): Flow<List<BucketItem>> {
        return bucketDao.getTodoItems(collection.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getMemories(): Flow<List<BucketItem>> {
        return bucketDao.getMemories().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getSecretMemories(): Flow<List<BucketItem>> {
        return bucketDao.getSecretMemories().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getTrashItems(): Flow<List<BucketItem>> {
        return bucketDao.getTrashItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getItemById(id: String): BucketItem? {
        return bucketDao.getItemById(id)?.toDomain()
    }

    fun getItemByIdFlow(id: String): Flow<BucketItem?> {
        return bucketDao.getItemByIdFlow(id).map { it?.toDomain() }
    }

    suspend fun createItem(item: BucketItem) {
        val now = System.currentTimeMillis()
        val deviceId = dataStoreManager.getOrCreateDeviceId()
        val userSub = dataStoreManager.userSubFlow.first()
        val userName = dataStoreManager.userNameFlow.first()

        val finalItem = item.copy(
            createdBy = if (item.createdBy.isEmpty()) userSub.ifEmpty { deviceId } else item.createdBy,
            createdByName = if (item.createdByName.isEmpty()) userName else item.createdByName,
            createdAt = if (item.createdAt == 0L) now else item.createdAt,
            updatedAt = now
        )

        bucketDao.upsert(BucketItemEntity.fromDomain(finalItem))

        if (finalItem.collection == BucketCollection.SHARED || finalItem.collection == BucketCollection.HEART) {
            val seq = dataStoreManager.getNextDeviceSequence()
            val op = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = finalItem.collection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = finalItem.id,
                kind = OpKind.CREATE,
                itemSnapshotJson = json.encodeToString(finalItem),
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(op))
        }

        val isReminderOn = dataStoreManager.isReminderEnabled(finalItem.id)
        reminderScheduler.scheduleRemindersForItem(finalItem, isReminderOn)
    }

    suspend fun updateItem(newItem: BucketItem) {
        val existing = getItemById(newItem.id) ?: return
        val now = System.currentTimeMillis()
        val finalItem = newItem.copy(updatedAt = now)
        bucketDao.upsert(BucketItemEntity.fromDomain(finalItem))

        if (finalItem.collection == BucketCollection.SHARED || finalItem.collection == BucketCollection.HEART) {
            val deviceId = dataStoreManager.getOrCreateDeviceId()
            val userSub = dataStoreManager.userSubFlow.first()
            val userName = dataStoreManager.userNameFlow.first()
            val seq = dataStoreManager.getNextDeviceSequence()

            val op = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = finalItem.collection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = finalItem.id,
                kind = OpKind.UPDATE_FIELD,
                itemSnapshotJson = json.encodeToString(finalItem),
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(op))
        }

        val isReminderOn = dataStoreManager.isReminderEnabled(finalItem.id)
        reminderScheduler.scheduleRemindersForItem(finalItem, isReminderOn)
    }

    suspend fun toggleChecklistItem(itemId: String, checkId: String, checked: Boolean) {
        val item = getItemById(itemId) ?: return
        val updatedChecklist = item.checklist.map {
            if (it.id == checkId) it.copy(checked = checked) else it
        }
        val now = System.currentTimeMillis()
        val updatedItem = item.copy(checklist = updatedChecklist, updatedAt = now)
        bucketDao.upsert(BucketItemEntity.fromDomain(updatedItem))

        if (item.collection == BucketCollection.SHARED || item.collection == BucketCollection.HEART) {
            val deviceId = dataStoreManager.getOrCreateDeviceId()
            val userSub = dataStoreManager.userSubFlow.first()
            val userName = dataStoreManager.userNameFlow.first()
            val seq = dataStoreManager.getNextDeviceSequence()

            val op = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = item.collection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = item.id,
                kind = OpKind.CHECKLIST_TOGGLE,
                fieldPath = checkId,
                fieldValueJson = checked.toString(),
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(op))
        }
    }

    suspend fun completeItem(itemId: String) {
        val item = getItemById(itemId) ?: return
        val now = System.currentTimeMillis()
        val completedItem = item.copy(
            status = BucketStatus.DONE,
            completedAt = now,
            updatedAt = now
        )
        bucketDao.upsert(BucketItemEntity.fromDomain(completedItem))
        reminderScheduler.cancelRemindersForItem(itemId)

        if (item.collection == BucketCollection.SHARED || item.collection == BucketCollection.HEART) {
            val deviceId = dataStoreManager.getOrCreateDeviceId()
            val userSub = dataStoreManager.userSubFlow.first()
            val userName = dataStoreManager.userNameFlow.first()
            val seq = dataStoreManager.getNextDeviceSequence()

            val op = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = item.collection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = item.id,
                kind = OpKind.COMPLETE,
                fieldValueJson = now.toString(),
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(op))
        }
    }

    suspend fun uncompleteItem(itemId: String) {
        val item = getItemById(itemId) ?: return
        val now = System.currentTimeMillis()
        val uncompletedItem = item.copy(
            status = BucketStatus.TODO,
            completedAt = null,
            updatedAt = now
        )
        bucketDao.upsert(BucketItemEntity.fromDomain(uncompletedItem))

        val isReminderOn = dataStoreManager.isReminderEnabled(itemId)
        reminderScheduler.scheduleRemindersForItem(uncompletedItem, isReminderOn)

        if (item.collection == BucketCollection.SHARED || item.collection == BucketCollection.HEART) {
            val deviceId = dataStoreManager.getOrCreateDeviceId()
            val userSub = dataStoreManager.userSubFlow.first()
            val userName = dataStoreManager.userNameFlow.first()
            val seq = dataStoreManager.getNextDeviceSequence()

            val op = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = item.collection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = item.id,
                kind = OpKind.UNCOMPLETE,
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(op))
        }
    }

    suspend fun doAgain(originalItemId: String, newTargetDate: String?): BucketItem? {
        val original = getItemById(originalItemId) ?: return null
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Reset checklist checkmarks
        val uncheckList = original.checklist.map { it.copy(checked = false) }

        val copyItem = original.copy(
            id = newId,
            targetDate = newTargetDate,
            checklist = uncheckList,
            status = BucketStatus.TODO,
            completedAt = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            clonedFromId = original.id
        )

        createItem(copyItem)
        return copyItem
    }

    suspend fun moveToTrash(itemId: String) {
        val item = getItemById(itemId) ?: return
        val now = System.currentTimeMillis()
        val trashedItem = item.copy(deletedAt = now, updatedAt = now)
        bucketDao.upsert(BucketItemEntity.fromDomain(trashedItem))
        reminderScheduler.cancelRemindersForItem(itemId)

        if (item.collection == BucketCollection.SHARED || item.collection == BucketCollection.HEART) {
            val deviceId = dataStoreManager.getOrCreateDeviceId()
            val userSub = dataStoreManager.userSubFlow.first()
            val userName = dataStoreManager.userNameFlow.first()
            val seq = dataStoreManager.getNextDeviceSequence()

            val op = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = item.collection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = item.id,
                kind = OpKind.DELETE,
                fieldValueJson = now.toString(),
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(op))
        }
    }

    suspend fun restoreFromTrash(itemId: String) {
        val item = getItemById(itemId) ?: return
        val now = System.currentTimeMillis()
        val restoredItem = item.copy(deletedAt = null, updatedAt = now)
        bucketDao.upsert(BucketItemEntity.fromDomain(restoredItem))

        val isReminderOn = dataStoreManager.isReminderEnabled(itemId)
        reminderScheduler.scheduleRemindersForItem(restoredItem, isReminderOn)

        if (item.collection == BucketCollection.SHARED || item.collection == BucketCollection.HEART) {
            val deviceId = dataStoreManager.getOrCreateDeviceId()
            val userSub = dataStoreManager.userSubFlow.first()
            val userName = dataStoreManager.userNameFlow.first()
            val seq = dataStoreManager.getNextDeviceSequence()

            val op = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = item.collection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = item.id,
                kind = OpKind.RESTORE,
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(op))
        }
    }

    suspend fun deletePermanently(itemId: String) {
        bucketDao.deletePermanently(itemId)
        reminderScheduler.cancelRemindersForItem(itemId)
    }

    suspend fun purgeOldTrash(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
        return bucketDao.purgeTrashOlderThan(thirtyDaysAgo)
    }

    suspend fun moveCollection(itemId: String, targetCollection: BucketCollection) {
        val item = getItemById(itemId) ?: return
        if (item.collection == targetCollection) return

        val fromCollection = item.collection
        val now = System.currentTimeMillis()
        val moveId = UUID.randomUUID().toString()
        val movedItem = item.copy(collection = targetCollection, updatedAt = now)
        bucketDao.upsert(BucketItemEntity.fromDomain(movedItem))

        val deviceId = dataStoreManager.getOrCreateDeviceId()
        val userSub = dataStoreManager.userSubFlow.first()
        val userName = dataStoreManager.userNameFlow.first()

        // 1. If previously in SHARED or HEART, notify partner it was removed/moved
        if (fromCollection == BucketCollection.SHARED || fromCollection == BucketCollection.HEART) {
            val seq = dataStoreManager.getNextDeviceSequence()
            val deleteOp = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = fromCollection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = item.id,
                kind = OpKind.DELETE,
                moveId = moveId,
                fieldValueJson = "moved_to_${targetCollection.name}",
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(deleteOp))
        }

        // 2. If newly in SHARED or HEART, upload it to partner
        if (targetCollection == BucketCollection.SHARED || targetCollection == BucketCollection.HEART) {
            val seq = dataStoreManager.getNextDeviceSequence()
            val createOp = SyncOperation(
                opId = UUID.randomUUID().toString(),
                collection = targetCollection,
                deviceId = deviceId,
                actorSub = userSub,
                actorName = userName,
                itemId = item.id,
                kind = OpKind.CREATE,
                moveId = moveId,
                itemSnapshotJson = json.encodeToString(movedItem),
                createdAt = now,
                deviceSequence = seq
            )
            syncDao.insert(SyncOperationEntity.fromDomain(createOp))
        }
    }
}
