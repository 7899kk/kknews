package com.karthikkilari.expiryradar.workers

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.karthikkilari.expiryradar.MainActivity
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.RadarApplication
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import java.time.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ReminderScheduler {
    const val TAG = "expiry-reminders"

    fun reconcile(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork(
            "expiry-daily",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).addTag(TAG).build(),
        )
        manager.enqueueUniqueWork(
            "expiry-reconcile",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().addTag(TAG).build(),
        )
        val now = ZonedDateTime.now()
        val next = now.toLocalDate().plusDays(1).atTime(9, 0).atZone(now.zone)
        manager.enqueueUniqueWork(
            "expiry-morning",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun cancelItem(context: Context, id: String) {
        WorkManager.getInstance(context).cancelUniqueWork("snooze-$id")
    }

    fun dismiss(context: Context, id: String) {
        NotificationManagerCompat.from(context).cancel(id, 0)
    }

    fun allowed(context: Context) =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
}

class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as RadarApplication
        return try {
            val prefs = app.preferences.flow.first()
            if (!prefs.notifications || !ReminderScheduler.allowed(app)) return Result.success()
            val nm = app.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    "expiry",
                    "Expiry reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
            val today = LocalDate.now()
            val snooze = inputData.getString("snooze")
            for (item in
                app.repository.dao.all().filter {
                    it.state == "ACTIVE" && (snooze == null || it.id == snooze)
                }) {
                val due =
                    ReminderPolicy.due(
                        LocalDate.parse(item.expiry),
                        ReminderPolicy.parse(item.reminders),
                        today,
                    )
                if (due.isEmpty() && snooze == null) continue
                val key =
                    if (snooze == null) "${item.id}:${item.expiry}:$today"
                    else "${item.id}:snooze:${inputData.getLong("stamp",0)}"
                val message =
                    "${item.name}: ${Expiry.label(LocalDate.parse(item.expiry),today).lowercase()}"
                if (app.repository.dao.history(History(key, item.id, item.name, message)) == -1L)
                    continue
                val open =
                    PendingIntent.getActivity(
                        app,
                        0,
                        Intent(app, MainActivity::class.java)
                            .setData(android.net.Uri.parse("expiryradar://item/${item.id}"))
                            .putExtra("itemId", item.id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                fun action(state: String): PendingIntent =
                    PendingIntent.getBroadcast(
                        app,
                        0,
                        Intent(app, ReminderActionReceiver::class.java)
                            .setAction(state)
                            .setData(android.net.Uri.parse("expiryradar://$state/${item.id}"))
                            .putExtra("itemId", item.id),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                try {
                    NotificationManagerCompat.from(app)
                        .notify(
                            item.id,
                            0,
                            NotificationCompat.Builder(app, "expiry")
                                .setSmallIcon(R.drawable.ic_notification)
                                .setContentTitle("Expiry Radar")
                                .setContentText(message)
                                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                                .setContentIntent(open)
                                .setAutoCancel(true)
                                .setGroup("expiry")
                                .addAction(0, "Used", action("USED"))
                                .addAction(0, "Discarded", action("DISCARDED"))
                                .addAction(0, "Snooze 1 hour", action("SNOOZE"))
                                .build(),
                        )
                } catch (e: SecurityException) {
                    app.repository.dao.deleteHistory(key)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("itemId") ?: return
        val app = context.applicationContext as RadarApplication
        val pending = goAsync()
        app.scope.launch {
            try {
                if (intent.action == "SNOOZE") {
                    WorkManager.getInstance(app)
                        .enqueueUniqueWork(
                            "snooze-$id",
                            ExistingWorkPolicy.REPLACE,
                            OneTimeWorkRequestBuilder<ReminderWorker>()
                                .setInputData(
                                    workDataOf(
                                        "snooze" to id,
                                        "stamp" to System.currentTimeMillis(),
                                    )
                                )
                                .setInitialDelay(1, TimeUnit.HOURS)
                                .addTag(ReminderScheduler.TAG)
                                .build(),
                        )
                    ReminderScheduler.dismiss(app, id)
                } else {
                    app.repository.dao.item(id)?.let {
                        app.repository.state(
                            it,
                            if (intent.action == "USED") "USED" else "DISCARDED",
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class ClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> ReminderScheduler.reconcile(context)
        }
    }
}
