package com.karthikkilari.expiryradar

import android.app.Application
import androidx.room.Room
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.workers.ReminderScheduler
import kotlinx.coroutines.*

open class RadarApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy {
        Room.databaseBuilder(this, RadarDatabase::class.java, "expiry-radar.db").build()
    }
    val repository by lazy { Repository(this, database) }
    val preferences by lazy { PreferenceStore(this) }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            repository.seed()
            ReminderScheduler.reconcile(this@RadarApplication)
        }
    }
}
