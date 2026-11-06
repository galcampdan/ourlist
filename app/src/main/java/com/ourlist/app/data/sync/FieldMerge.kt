package com.ourlist.app.data.sync

import com.ourlist.app.data.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class FieldDifference(val path: String, val local: String?, val remote: String?)
internal data class FieldMergeResult(val item: BucketItem, val conflicts: List<FieldDifference>)

internal object FieldMerge {
    private val json = Json { ignoreUnknownKeys = true }
    fun merge(local: BucketItem, op: SyncOperation, pendingFields: Set<String>): FieldMergeResult {
        val remote = json.decodeFromString<BucketItem>(requireNotNull(op.itemSnapshotJson))
        require(remote.id == local.id && remote.collection in setOf(BucketCollection.SHARED, BucketCollection.HEART))
        val base = op.baseSnapshotJson?.let { json.decodeFromString<BucketItem>(it) }
        val fields = op.fieldPath?.split(',')?.map { it.trim() }?.toSet().orEmpty()
        val conflicts = mutableListOf<FieldDifference>()
        fun accept(path: String, l: String?, v: String?, b: String?): Boolean {
            if (path !in fields || l == v) return false
            val concurrent = if (base != null) l != b && v != b else path in pendingFields
            if (concurrent && !op.resolvesConflict) { conflicts.add(FieldDifference(path, l, v)); return false }
            return true
        }
        val result = local.copy(
            title = if (accept("title",local.title,remote.title,base?.title)) remote.title else local.title,
            wishType = if (accept("wishType",local.wishType.name,remote.wishType.name,base?.wishType?.name)) remote.wishType else local.wishType,
            targetDate = if (accept("targetDate",local.targetDate,remote.targetDate,base?.targetDate)) remote.targetDate else local.targetDate,
            estimatedCostKRW = if (accept("estimatedCostKRW",local.estimatedCostKRW?.toString(),remote.estimatedCostKRW?.toString(),base?.estimatedCostKRW?.toString())) remote.estimatedCostKRW else local.estimatedCostKRW,
            placeText = if (accept("placeText",local.placeText,remote.placeText,base?.placeText)) remote.placeText else local.placeText,
            memo = if (accept("memo",local.memo,remote.memo,base?.memo)) remote.memo else local.memo,
            checklist = if (accept("checklist",json.encodeToString(local.checklist),json.encodeToString(remote.checklist),base?.let { json.encodeToString(it.checklist) })) remote.checklist else local.checklist,
            completedAt = if (accept("completedAt",local.completedAt?.toString(),remote.completedAt?.toString(),base?.completedAt?.toString())) remote.completedAt else local.completedAt,
            updatedAt = maxOf(local.updatedAt, op.createdAt)
        )
        return FieldMergeResult(result, conflicts)
    }
}

internal fun orderedOperations(operations: List<SyncOperation>): List<SyncOperation> {
    val queues = operations.distinctBy { it.opId }.groupBy { it.deviceId }.values
        .map { it.sortedWith(compareBy<SyncOperation> { it.deviceSequence }.thenBy { it.createdAt }.thenBy { it.opId }).toMutableList() }.toMutableList()
    val result = mutableListOf<SyncOperation>()
    while (queues.isNotEmpty()) {
        val q = queues.minWith(compareBy<MutableList<SyncOperation>> { it.first().createdAt }.thenBy { it.first().deviceId }.thenBy { it.first().opId })
        result.add(q.removeAt(0))
        if (q.isEmpty()) queues.remove(q)
    }
    return result
}
