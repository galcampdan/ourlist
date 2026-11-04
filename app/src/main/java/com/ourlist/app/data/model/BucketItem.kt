package com.ourlist.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean = false
)

@Serializable
data class BucketItem(
    val id: String,
    val collection: BucketCollection,
    val title: String,
    val wishType: WishType = WishType.MUST_DO,
    val targetDate: String? = null, // YYYY-MM-DD
    val estimatedCostKRW: Long? = null,
    val placeText: String? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val memo: String? = null,
    val status: BucketStatus = BucketStatus.TODO,
    val completedAt: Long? = null,
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val clonedFromId: String? = null,
    val schemaVersion: Int = 1
) {
    val isDeleted: Boolean
        get() = deletedAt != null

    val isDone: Boolean
        get() = status == BucketStatus.DONE
}
