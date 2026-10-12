package com.karthikkilari.expiryradar

import android.app.NotificationManager
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.testing.*
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.workers.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestRadarApplication::class)
class ReminderIntegrationTest {
    private lateinit var app: TestRadarApplication

    @Before
    fun setup() = runBlocking {
        app = RuntimeEnvironment.getApplication() as TestRadarApplication
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        app.repository.dao.clearItems()
        app.repository.dao.clearHistory()
        app.preferences.set(Preferences(onboarded = true))
        app.getSystemService(NotificationManager::class.java).cancelAll()
    }

    @Test
    fun dueNotificationPostsOnceAndTargetsItem() = runBlocking {
        val item =
            Item(name = "Milk", expiry = LocalDate.now().plusDays(1).toString(), reminders = "1")
        app.repository.dao.save(item)
        val first = TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertEquals(ListenableWorker.Result.success(), first)
        assertEquals(1, app.repository.dao.allHistory().size)
        val shadow = shadowOf(app.getSystemService(NotificationManager::class.java))
        assertEquals(1, shadow.allNotifications.size)
        val notification = shadow.allNotifications.single()
        assertEquals(
            "Expiry Radar",
            notification.extras.getCharSequence("android.title").toString(),
        )
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertEquals(item.id, intent.getStringExtra("itemId"))
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertEquals(1, app.repository.dao.allHistory().size)
        assertEquals(1, shadow.allNotifications.size)
    }

    @Test
    fun disabledNotificationsDoNotRecordHistory() = runBlocking {
        app.preferences.set(Preferences(onboarded = true, notifications = false))
        app.repository.dao.save(
            Item(name = "Milk", expiry = LocalDate.now().toString(), reminders = "0")
        )
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertTrue(app.repository.dao.allHistory().isEmpty())
        assertTrue(
            shadowOf(app.getSystemService(NotificationManager::class.java))
                .allNotifications
                .isEmpty()
        )
    }

    @Test
    fun usedItemsDoNotNotify() = runBlocking {
        app.repository.dao.save(
            Item(
                name = "Milk",
                expiry = LocalDate.now().toString(),
                reminders = "0",
                state = "USED",
            )
        )
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertTrue(app.repository.dao.allHistory().isEmpty())
    }

    @Test
    fun deleteCancelsVisibleNotification() = runBlocking {
        val item = Item(name = "Milk", expiry = LocalDate.now().toString(), reminders = "0")
        app.repository.dao.save(item)
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        app.repository.delete(item)
        assertTrue(
            shadowOf(app.getSystemService(NotificationManager::class.java))
                .allNotifications
                .isEmpty()
        )
    }

    @Test
    @Config(sdk = [35])
    fun notificationPermissionDenialIsSafe() = runBlocking {
        shadowOf(app).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        app.repository.dao.save(
            Item(name = "Milk", expiry = LocalDate.now().toString(), reminders = "0")
        )
        assertFalse(ReminderScheduler.allowed(app))
        TestListenableWorkerBuilder<ReminderWorker>(app).build().doWork()
        assertTrue(app.repository.dao.allHistory().isEmpty())
    }
}
