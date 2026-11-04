package com.ourlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ourlist.app.data.local.entity.ConflictEntity
import com.ourlist.app.data.local.entity.InAppNotificationEntity
import com.ourlist.app.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Query("SELECT * FROM sync_operations WHERE isUploaded = 0 ORDER BY deviceSequence ASC, createdAt ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE isUploaded = 0 ORDER BY deviceSequence ASC, createdAt ASC")
    fun getPendingOperationsFlow(): Flow<List<SyncOperationEntity>>

    @Query("SELECT COUNT(*) FROM sync_operations WHERE isUploaded = 0")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: SyncOperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ops: List<SyncOperationEntity>)

    @Query("UPDATE sync_operations SET isUploaded = 1 WHERE opId = :opId")
    suspend fun markUploaded(opId: String)

    @Query("SELECT * FROM sync_operations WHERE isUploaded = 0 AND itemId = :itemId")
    suspend fun getPendingOpsForItem(itemId: String): List<SyncOperationEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM sync_operations WHERE opId = :opId)")
    suspend fun exists(opId: String): Boolean

    @Query("DELETE FROM sync_operations WHERE isUploaded = 1 AND createdAt < :cutoffMillis")
    suspend fun purgeUploadedOlderThan(cutoffMillis: Long): Int
}

@Dao
interface ConflictDao {

    @Query("SELECT * FROM conflicts WHERE isResolved = 0 ORDER BY createdAt DESC")
    fun getUnresolvedConflictsFlow(): Flow<List<ConflictEntity>>

    @Query("SELECT * FROM conflicts WHERE isResolved = 0 ORDER BY createdAt DESC")
    suspend fun getUnresolvedConflicts(): List<ConflictEntity>

    @Query("SELECT COUNT(*) FROM conflicts WHERE isResolved = 0")
    fun getUnresolvedCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: ConflictEntity)

    @Query("UPDATE conflicts SET isResolved = 1 WHERE conflictId = :conflictId")
    suspend fun resolveConflict(conflictId: String)

    @Query("DELETE FROM conflicts WHERE itemId = :itemId")
    suspend fun deleteFor(itemId: String)
}

@Dao
interface InAppNotificationDao {

    @Query("SELECT * FROM in_app_notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<InAppNotificationEntity>>

    @Query("SELECT COUNT(*) FROM in_app_notifications WHERE isRead = 0")
    fun getUnreadCountFlow(): Flow<Int>

    @Query("SELECT * FROM in_app_notifications WHERE itemId = :itemId AND timestamp > :thresholdMillis ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentForSameItem(itemId: String, thresholdMillis: Long): InAppNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: InAppNotificationEntity)

    @Update
    suspend fun update(notification: InAppNotificationEntity)

    @Query("UPDATE in_app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE in_app_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM in_app_notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM in_app_notifications")
    suspend fun deleteAll()
}
