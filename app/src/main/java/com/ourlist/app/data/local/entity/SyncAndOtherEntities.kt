package com.ourlist.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.ConflictRecord
import com.ourlist.app.data.model.ConflictType
import com.ourlist.app.data.model.InAppNotification
import com.ourlist.app.data.model.OpKind
import com.ourlist.app.data.model.SyncOperation

@Entity(
    tableName = "sync_operations",
    indices = [
        Index(value = ["isUploaded"]),
        Index(value = ["collection"]),
        Index(value = ["itemId"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOperationEntity(
    @PrimaryKey
    val opId: String,
    val collection: String,
    val deviceId: String,
    val actorSub: String,
    val actorName: String,
    val itemId: String,
    val kind: String,
    val fieldPath: String?,
    val fieldValueJson: String?,
    val itemSnapshotJson: String?,
    val createdAt: Long,
    val deviceSequence: Long,
    val moveId: String?,
    val schemaVersion: Int,
    val isUploaded: Boolean = false
) {
    fun toDomain(): SyncOperation = SyncOperation(
        opId = opId,
        collection = BucketCollection.valueOf(collection),
        deviceId = deviceId,
        actorSub = actorSub,
        actorName = actorName,
        itemId = itemId,
        kind = OpKind.valueOf(kind),
        fieldPath = fieldPath,
        fieldValueJson = fieldValueJson,
        itemSnapshotJson = itemSnapshotJson,
        createdAt = createdAt,
        deviceSequence = deviceSequence,
        moveId = moveId,
        schemaVersion = schemaVersion
    )

    companion object {
        fun fromDomain(op: SyncOperation, isUploaded: Boolean = false): SyncOperationEntity =
            SyncOperationEntity(
                opId = op.opId,
                collection = op.collection.name,
                deviceId = op.deviceId,
                actorSub = op.actorSub,
                actorName = op.actorName,
                itemId = op.itemId,
                kind = op.kind.name,
                fieldPath = op.fieldPath,
                fieldValueJson = op.fieldValueJson,
                itemSnapshotJson = op.itemSnapshotJson,
                createdAt = op.createdAt,
                deviceSequence = op.deviceSequence,
                moveId = op.moveId,
                schemaVersion = op.schemaVersion,
                isUploaded = isUploaded
            )
    }
}

@Entity(tableName = "conflicts")
data class ConflictEntity(
    @PrimaryKey
    val conflictId: String,
    val itemId: String,
    val itemTitle: String,
    val collection: String,
    val conflictType: String,
    val fieldPath: String?,
    val localValue: String?,
    val remoteValue: String?,
    val remoteActorName: String,
    val remoteUpdatedAt: Long,
    val createdAt: Long,
    val isResolved: Boolean
) {
    fun toDomain(): ConflictRecord = ConflictRecord(
        conflictId = conflictId,
        itemId = itemId,
        itemTitle = itemTitle,
        collection = BucketCollection.valueOf(collection),
        conflictType = ConflictType.valueOf(conflictType),
        fieldPath = fieldPath,
        localValue = localValue,
        remoteValue = remoteValue,
        remoteActorName = remoteActorName,
        remoteUpdatedAt = remoteUpdatedAt,
        createdAt = createdAt,
        isResolved = isResolved
    )

    companion object {
        fun fromDomain(c: ConflictRecord): ConflictEntity = ConflictEntity(
            conflictId = c.conflictId,
            itemId = c.itemId,
            itemTitle = c.itemTitle,
            collection = c.collection.name,
            conflictType = c.conflictType.name,
            fieldPath = c.fieldPath,
            localValue = c.localValue,
            remoteValue = c.remoteValue,
            remoteActorName = c.remoteActorName,
            remoteUpdatedAt = c.remoteUpdatedAt,
            createdAt = c.createdAt,
            isResolved = c.isResolved
        )
    }
}

@Entity(tableName = "in_app_notifications")
data class InAppNotificationEntity(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val collection: String,
    val actorName: String,
    val actionType: String,
    val message: String,
    val editCount: Int,
    val timestamp: Long,
    val isRead: Boolean
) {
    fun toDomain(): InAppNotification = InAppNotification(
        id = id,
        itemId = itemId,
        itemTitle = itemTitle,
        collection = BucketCollection.valueOf(collection),
        actorName = actorName,
        actionType = actionType,
        message = message,
        editCount = editCount,
        timestamp = timestamp,
        isRead = isRead
    )

    companion object {
        fun fromDomain(n: InAppNotification): InAppNotificationEntity = InAppNotificationEntity(
            id = n.id,
            itemId = n.itemId,
            itemTitle = n.itemTitle,
            collection = n.collection.name,
            actorName = n.actorName,
            actionType = n.actionType,
            message = n.message,
            editCount = n.editCount,
            timestamp = n.timestamp,
            isRead = n.isRead
        )
    }
}
