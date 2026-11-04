package com.ourlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ourlist.app.data.local.entity.BucketItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BucketDao {

    @Query("SELECT * FROM bucket_items WHERE collection = :collection AND status = 'TODO' AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getTodoItems(collection: String): Flow<List<BucketItemEntity>>

    @Query("SELECT * FROM bucket_items WHERE status = 'DONE' AND deletedAt IS NULL AND collection != 'SECRET_LOCAL' ORDER BY completedAt DESC")
    fun getMemories(): Flow<List<BucketItemEntity>>

    @Query("SELECT * FROM bucket_items WHERE status = 'DONE' AND deletedAt IS NULL AND collection = 'SECRET_LOCAL' ORDER BY completedAt DESC")
    fun getSecretMemories(): Flow<List<BucketItemEntity>>

    @Query("SELECT * FROM bucket_items WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashItems(): Flow<List<BucketItemEntity>>

    @Query("SELECT * FROM bucket_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): BucketItemEntity?

    @Query("SELECT * FROM bucket_items WHERE id = :id LIMIT 1")
    fun getItemByIdFlow(id: String): Flow<BucketItemEntity?>

    @Query("SELECT * FROM bucket_items WHERE deletedAt IS NULL AND (targetDate IS NOT NULL AND targetDate != '')")
    suspend fun getItemsWithTargetDate(): List<BucketItemEntity>

    @Query("SELECT * FROM bucket_items WHERE collection = :collection AND deletedAt IS NULL")
    suspend fun getAllInCollection(collection: String): List<BucketItemEntity>

    @Query("SELECT * FROM bucket_items")
    suspend fun getAllItems(): List<BucketItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: BucketItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<BucketItemEntity>)

    @Update
    suspend fun update(item: BucketItemEntity)

    @Query("DELETE FROM bucket_items WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM bucket_items WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffMillis")
    suspend fun purgeTrashOlderThan(cutoffMillis: Long): Int
}
