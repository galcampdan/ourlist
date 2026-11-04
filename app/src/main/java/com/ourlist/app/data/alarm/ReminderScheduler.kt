package com.ourlist.app.data.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ourlist.app.MainActivity
import com.ourlist.app.R
import com.ourlist.app.data.local.OurListDatabase
import com.ourlist.app.data.local.DataStoreManager
import com.ourlist.app.data.model.BucketItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val CHANNEL_ID = "bucket_reminder_channel"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_TITLE = "extra_item_title"
        const val EXTRA_DAYS_LEFT = "extra_days_left" // 7 or 0

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleRemindersForItem(item: BucketItem, isEnabled: Boolean) {
        cancelRemindersForItem(item.id)
        if (!isEnabled || item.isDone || item.isDeleted || item.targetDate.isNullOrEmpty()) {
            return
        }

        try {
            val date = LocalDate.parse(item.targetDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()

            // 1. D-7 09:00 AM
            val dMinus7Time = date.minusDays(7).atTime(9, 0)
                .atZone(zone).toInstant().toEpochMilli()
            if (dMinus7Time > now) {
                scheduleAlarm(item.id, item.title, 7, dMinus7Time)
            }

            // 2. D-Day 09:00 AM
            val dDayTime = date.atTime(9, 0)
                .atZone(zone).toInstant().toEpochMilli()
            if (dDayTime > now) {
                scheduleAlarm(item.id, item.title, 0, dDayTime)
            }
        } catch (e: Exception) {
            // Ignore date parsing exceptions
        }
    }

    fun cancelRemindersForItem(itemId: String) {
        cancelAlarm(itemId, 7)
        cancelAlarm(itemId, 0)
    }

    private fun scheduleAlarm(itemId: String, title: String, daysLeft: Int, triggerAtMillis: Long) {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_ITEM_TITLE, title)
            putExtra(EXTRA_DAYS_LEFT, daysLeft)
        }
        val requestCode = generateRequestCode(itemId, daysLeft)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(itemId: String, daysLeft: Int) {
        val intent = Intent(context, ReminderNotificationReceiver::class.java)
        val requestCode = generateRequestCode(itemId, daysLeft)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun generateRequestCode(itemId: String, daysLeft: Int): Int {
        return (itemId.hashCode() * 31 + daysLeft) and 0x7FFFFFFF
    }
}

class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(ReminderScheduler.EXTRA_ITEM_ID) ?: return
        val itemTitle = intent.getStringExtra(ReminderScheduler.EXTRA_ITEM_TITLE) ?: "버킷리스트"
        val daysLeft = intent.getIntExtra(ReminderScheduler.EXTRA_DAYS_LEFT, 0)

        val message = if (daysLeft == 7) {
            "목표 날짜가 일주일 남았어요! 미리 준비해보세요."
        } else {
            "오늘이 바로 그 날이에요! 멋진 추억을 만들어보세요."
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_item_id", itemId)
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            itemId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("[$itemTitle] $message")
            .setContentText(itemTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(itemId.hashCode() + daysLeft, notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        ) {
            val db = OurListDatabase.getInstance(context)
            val ds = DataStoreManager(context)
            val scheduler = ReminderScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val itemsWithDate = db.bucketDao().getItemsWithTargetDate()
                for (itemEntity in itemsWithDate) {
                    val item = itemEntity.toDomain()
                    val enabled = ds.isReminderEnabled(item.id)
                    scheduler.scheduleRemindersForItem(item, enabled)
                }
            }
        }
    }
}
