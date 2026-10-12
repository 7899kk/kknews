package com.karthikkilari.expiryradar.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

val Context.radarPrefs by preferencesDataStore("radar_preferences")

@Serializable
data class Preferences(
    val onboarded: Boolean = false,
    val theme: String = "System",
    val warning: Int = 7,
    val reminders: String = "7,3,1,0",
    val notifications: Boolean = true,
    val dateFormat: String = "dd MMM yyyy",
    val mondayFirst: Boolean = true,
)

class PreferenceStore(private val context: Context) {
    val flow =
        context.radarPrefs.data.map { p ->
            Preferences(
                p[booleanPreferencesKey("onboarded")] ?: false,
                p[stringPreferencesKey("theme")] ?: "System",
                p[intPreferencesKey("warning")] ?: 7,
                p[stringPreferencesKey("reminders")] ?: "7,3,1,0",
                p[booleanPreferencesKey("notifications")] ?: true,
                p[stringPreferencesKey("dateFormat")] ?: "dd MMM yyyy",
                p[booleanPreferencesKey("mondayFirst")] ?: true,
            )
        }

    suspend fun set(value: Preferences) {
        context.radarPrefs.edit { p ->
            p[booleanPreferencesKey("onboarded")] = value.onboarded
            p[stringPreferencesKey("theme")] = value.theme
            p[intPreferencesKey("warning")] = value.warning
            p[stringPreferencesKey("reminders")] = value.reminders
            p[booleanPreferencesKey("notifications")] = value.notifications
            p[stringPreferencesKey("dateFormat")] = value.dateFormat
            p[booleanPreferencesKey("mondayFirst")] = value.mondayFirst
        }
    }
}
