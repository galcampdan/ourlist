package com.ourlist.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ourlist.app.data.local.dao.BucketDao
import com.ourlist.app.data.local.dao.ConflictDao
import com.ourlist.app.data.local.dao.InAppNotificationDao
import com.ourlist.app.data.local.dao.SyncOperationDao
import com.ourlist.app.data.local.entity.BucketItemEntity
import com.ourlist.app.data.local.entity.ConflictEntity
import com.ourlist.app.data.local.entity.InAppNotificationEntity
import com.ourlist.app.data.local.entity.SyncOperationEntity

@Database(
    entities = [
        BucketItemEntity::class,
        SyncOperationEntity::class,
        ConflictEntity::class,
        InAppNotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class OurListDatabase : RoomDatabase() {

    abstract fun bucketDao(): BucketDao
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun conflictDao(): ConflictDao
    abstract fun inAppNotificationDao(): InAppNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: OurListDatabase? = null

        fun getInstance(context: Context): OurListDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OurListDatabase::class.java,
                    "our_list.db"
                ).addMigrations(object : Migration(1, 2) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE sync_operations ADD COLUMN baseSnapshotJson TEXT DEFAULT NULL")
                        db.execSQL("ALTER TABLE sync_operations ADD COLUMN resolvesConflict INTEGER NOT NULL DEFAULT 0")
                    }
                })
                 .build()
                 .also { INSTANCE = it }
            }
        }
    }
}
