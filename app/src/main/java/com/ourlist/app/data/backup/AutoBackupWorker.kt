package com.ourlist.app.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ourlist.app.OurListApp
import kotlinx.coroutines.flow.first

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? OurListApp ?: return Result.failure()
        val isAutoBackupOn = app.dataStoreManager.autoBackupEnabledFlow.first()
        if (!isAutoBackupOn) return Result.success()

        val lastBackupTime = app.dataStoreManager.lastBackupTimeFlow.first()
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L

        // Only run if 24 hours have elapsed
        if (now - lastBackupTime >= twentyFourHours) {
            val recoveryCode = app.dataStoreManager.recoveryCodeFlow.first()
            if (recoveryCode.isNotEmpty()) {
                val success = app.backupManager.performAutoDriveBackup(recoveryCode)
                if (success) {
                    app.dataStoreManager.setLastBackupTime(now)
                }
            }
        }
        return Result.success()
    }
}
