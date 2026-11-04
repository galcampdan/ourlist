package com.ourlist.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.model.BucketItem
import com.ourlist.app.data.model.BucketStatus
import com.ourlist.app.data.model.ChecklistItem
import com.ourlist.app.data.model.WishType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "bucket_items",
    indices = [
        Index(value = ["collection"]),
        Index(value = ["status"]),
        Index(value = ["deletedAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class BucketItemEntity(
    @PrimaryKey
    val id: String,
    val collection: String,
    val title: String,
    val wishType: String,
    val targetDate: String?,
    val estimatedCostKRW: Long?,
    val placeText: String?,
    val checklistJson: String,
    val memo: String?,
    val status: String,
    val completedAt: Long?,
    val createdBy: String,
    val createdByName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val clonedFromId: String?,
    val schemaVersion: Int
) {
    fun toDomain(): BucketItem {
        val checklist = try {
            Json.decodeFromString<List<ChecklistItem>>(checklistJson)
        } catch (e: Exception) {
            emptyList()
        }
        return BucketItem(
            id = id,
            collection = BucketCollection.valueOf(collection),
            title = title,
            wishType = WishType.valueOf(wishType),
            targetDate = targetDate,
            estimatedCostKRW = estimatedCostKRW,
            placeText = placeText,
            checklist = checklist,
            memo = memo,
            status = BucketStatus.valueOf(status),
            completedAt = completedAt,
            createdBy = createdBy,
            createdByName = createdByName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            clonedFromId = clonedFromId,
            schemaVersion = schemaVersion
        )
    }

    companion object {
        fun fromDomain(item: BucketItem): BucketItemEntity {
            val json = Json { ignoreUnknownKeys = true }
            return BucketItemEntity(
                id = item.id,
                collection = item.collection.name,
                title = item.title,
                wishType = item.wishType.name,
                targetDate = item.targetDate,
                estimatedCostKRW = item.estimatedCostKRW,
                placeText = item.placeText,
                checklistJson = json.encodeToString(item.checklist),
                memo = item.memo,
                status = item.status.name,
                completedAt = item.completedAt,
                createdBy = item.createdBy,
                createdByName = item.createdByName,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                deletedAt = item.deletedAt,
                clonedFromId = item.clonedFromId,
                schemaVersion = item.schemaVersion
            )
        }
    }
}
