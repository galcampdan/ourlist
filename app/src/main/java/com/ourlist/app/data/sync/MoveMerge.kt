package com.ourlist.app.data.sync

import com.ourlist.app.data.model.*
import kotlinx.serialization.json.Json

internal fun createOrMove(local: BucketItem?, op: SyncOperation): BucketItem? {
    require(op.collection in setOf(BucketCollection.SHARED, BucketCollection.HEART))
    if (local != null && op.moveId == null) return null
    require(local == null || local.collection in setOf(BucketCollection.SHARED, BucketCollection.HEART)) { "개인 항목과 공유 항목의 ID가 충돌합니다." }
    val snapshot = Json { ignoreUnknownKeys = true }.decodeFromString<BucketItem>(requireNotNull(op.itemSnapshotJson))
    require(snapshot.id == op.itemId && snapshot.collection == op.collection)
    return local?.copy(collection = op.collection, deletedAt = null, updatedAt = op.createdAt) ?: snapshot
}
