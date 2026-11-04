package com.ourlist.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConflictType {
    FIELD_CONFLICT,
    DELETE_VS_UPDATE
}

@Serializable
data class ConflictRecord(
    val conflictId: String,
    val itemId: String,
    val itemTitle: String,
    val collection: BucketCollection,
    val conflictType: ConflictType,
    val fieldPath: String? = null,
    val localValue: String? = null,
    val remoteValue: String? = null,
    val remoteActorName: String = "",
    val remoteUpdatedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

@Serializable
data class InAppNotification(
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val collection: BucketCollection,
    val actorName: String,
    val actionType: String,
    val message: String,
    val editCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
