package com.example.expensetracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val smsTrackingKey = booleanPreferencesKey("sms_tracking")

    val smsTrackingEnabled: Flow<Boolean> = context.dataStore.data.map { it[smsTrackingKey] ?: true }

    suspend fun setSmsTracking(enabled: Boolean) {
        context.dataStore.edit { it[smsTrackingKey] = enabled }
    }
}
