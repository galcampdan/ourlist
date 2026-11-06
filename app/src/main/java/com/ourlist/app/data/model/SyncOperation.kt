package com.ourlist.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class OpKind {
    CREATE,
    UPDATE_FIELD,
    CHECKLIST_TOGGLE,
    CHECKLIST_EDIT,
    COMPLETE,
    UNCOMPLETE,
    DELETE,
    RESTORE,
    MOVE
}

@Serializable
data class SyncOperation(
    val opId: String,
    val collection: BucketCollection, // SHARED or HEART
    val deviceId: String,
    val actorSub: String,
    val actorName: String,
    val itemId: String,
    val kind: OpKind,
    val fieldPath: String? = null,
    val fieldValueJson: String? = null,
    val itemSnapshotJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceSequence: Long = 0L,
    val moveId: String? = null,
    val baseSnapshotJson: String? = null,
    val resolvesConflict: Boolean = false,
    val schemaVersion: Int = 2
)
