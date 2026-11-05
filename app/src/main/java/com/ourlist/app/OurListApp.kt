package com.ourlist.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ourlist.app.data.alarm.ReminderScheduler
import com.ourlist.app.data.backup.AutoBackupWorker
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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
            val savedToken = dataStoreManager.googleAuthTokenFlow.first().ifBlank { null }
            if (savedToken != null) {
                savedToken
            } else {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this@OurListApp)
                if (account?.account != null) {
                    try {
                        val freshToken = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                            this@OurListApp,
                            account.account!!,
                            "oauth2:https://www.googleapis.com/auth/drive.file"
                        )
                        dataStoreManager.setGoogleAuthToken(freshToken)
                        freshToken
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
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

        // Auto-sync whenever there are local changes ("변동이 있을 때")
        scope.launch(Dispatchers.IO) {
            database.syncOperationDao().getPendingOperationsFlow().collect { pending ->
                if (pending.isNotEmpty()) {
                    syncEngine.sync()
                }
            }
        }

        // Initialize persistent device ID & run startup maintenance
        scope.launch(Dispatchers.IO) {
            dataStoreManager.getOrCreateDeviceId()
            bucketRepository.purgeOldTrash()

            val backupWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(this@OurListApp).enqueueUniquePeriodicWork(
                "OurListAutoBackup",
                ExistingPeriodicWorkPolicy.KEEP,
                backupWorkRequest
            )
        }
    }
}
