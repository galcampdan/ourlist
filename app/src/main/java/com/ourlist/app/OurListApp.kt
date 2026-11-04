package com.ourlist.app

import android.app.Application
import com.ourlist.app.data.alarm.ReminderScheduler
import com.ourlist.app.data.backup.BackupManager
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.repository.BucketRepository
import com.ourlist.app.data.repository.ConflictRepository
import com.ourlist.app.data.repository.NotificationRepository
import com.ourlist.app.data.sync.DriveSyncClient
import com.ourlist.app.data.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OurListApp : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: OurListDatabase
        private set

    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var reminderScheduler: ReminderScheduler
        private set

    lateinit var driveClient: DriveSyncClient
        private set

    lateinit var notificationRepository: NotificationRepository
        private set

    lateinit var conflictRepository: ConflictRepository
        private set

    lateinit var bucketRepository: BucketRepository
        private set

    lateinit var syncEngine: SyncEngine
        private set

    lateinit var backupManager: BackupManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = OurListDatabase.getInstance(this)
        dataStoreManager = DataStoreManager(this)
        reminderScheduler = ReminderScheduler(this)
        ReminderScheduler.createNotificationChannel(this)

        driveClient = DriveSyncClient {
            // Token provider: returns current auth token if connected
            null
        }

        notificationRepository = NotificationRepository(database)
        conflictRepository = ConflictRepository(database, dataStoreManager)
        bucketRepository = BucketRepository(database, dataStoreManager, reminderScheduler)

        syncEngine = SyncEngine(
            db = database,
            dataStoreManager = dataStoreManager,
            notificationRepository = notificationRepository,
            reminderScheduler = reminderScheduler,
            driveClient = driveClient
        )

        backupManager = BackupManager(
            db = database,
            dataStoreManager = dataStoreManager,
            driveClient = driveClient
        )

        // Initialize persistent device ID & run startup maintenance
        scope.launch(Dispatchers.IO) {
            dataStoreManager.getOrCreateDeviceId()
            bucketRepository.purgeOldTrash()
        }
    }
}
