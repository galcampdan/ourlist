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

    /**
     * Resolves human-readable creator name relative to the current device user.
     * Prevents partner's items from displaying "나" on my device.
     */
    fun getDisplayCreatorName(myDeviceId: String, myUserSub: String = "", partnerName: String = "상대방"): String {
        if (collection == BucketCollection.PERSONAL || collection == BucketCollection.SECRET_LOCAL) {
            return "나"
        }
        val isMe = (myDeviceId.isNotEmpty() && createdBy == myDeviceId) ||
                (myUserSub.isNotEmpty() && createdBy == myUserSub)
        return if (createdBy.isEmpty() || isMe) {
            "나"
        } else {
            if (partnerName.isNotBlank() && partnerName != "상대방") {
                partnerName
            } else if (createdByName.isNotBlank() && createdByName != "나") {
                createdByName
            } else {
                "상대방"
            }
        }
    }
}
