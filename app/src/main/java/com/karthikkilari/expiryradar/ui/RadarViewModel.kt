package com.karthikkilari.expiryradar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karthikkilari.expiryradar.RadarApplication
import com.karthikkilari.expiryradar.data.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class RadarViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as RadarApplication
    val repo = app.repository
    val items =
        repo.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val labels =
        repo.labels.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history =
        repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val prefs =
        app.preferences.flow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Preferences(),
        )
    val loaded = MutableStateFlow(false)
    val today = MutableStateFlow(LocalDate.now())
    val busy = MutableStateFlow(false)
    val messages = Channel<String>(Channel.BUFFERED)

    init {
        viewModelScope.launch {
            app.preferences.flow.first()
            repo.items.first()
            loaded.value = true
        }
        viewModelScope.launch {
            while (isActive) {
                today.value = LocalDate.now()
                delay(30000)
            }
        }
    }

    fun perform(success: String = "", block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                if (success.isNotEmpty()) messages.send(success)
            } catch (e: Exception) {
                messages.send(e.message ?: "Operation could not be completed")
            } finally {
                busy.value = false
            }
        }
    }

    fun settings(p: Preferences) {
        perform {
            app.preferences.set(p)
            if (!p.notifications)
                com.karthikkilari.expiryradar.workers.ReminderScheduler.cancel(app)
            com.karthikkilari.expiryradar.workers.ReminderScheduler.reconcile(app)
        }
    }
}
