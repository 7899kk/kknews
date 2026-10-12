@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.backup.BackupManager
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import com.karthikkilari.expiryradar.workers.ReminderScheduler
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

@Composable
internal fun Settings(
    vm: RadarViewModel,
    p: Preferences,
    labels: List<Label>,
    history: List<History>,
    busy: Boolean,
) {
    val context = LocalContext.current
    val backup = remember { BackupManager(vm.app) }
    var warning by rememberSaveable(p.warning) { mutableStateOf(p.warning.toString()) }
    var reminders by rememberSaveable(p.reminders) { mutableStateOf(p.reminders) }
    var category by rememberSaveable { mutableStateOf("") }
    var profile by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf("") }
    var restoreUri by rememberSaveable { mutableStateOf("") }
    var restoreType by rememberSaveable { mutableStateOf("") }
    val exportZip =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            if (uri != null) vm.perform("Backup exported") { backup.export(uri) }
        }
    val exportCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            if (uri != null) vm.perform("CSV exported") { backup.exportCsv(uri) }
        }
    val importZip =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                restoreUri = uri.toString()
                restoreType = "ZIP"
                dialog = "restore"
            }
        }
    val importCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                restoreUri = uri.toString()
                restoreType = "CSV"
                dialog = "restore"
            }
        }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) vm.settings(p.copy(notifications = true))
            else error = "Permission denied. Enable notifications later in Android Settings."
        }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Heading("Settings", "Offline, private, and yours.")
        Choice("Theme", p.theme, listOf("System", "Light", "Dark")) {
            vm.settings(p.copy(theme = it))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ui_enable_reminders), Modifier.weight(1f))
            Switch(
                p.notifications,
                { enabled ->
                    if (
                        enabled &&
                            Build.VERSION.SDK_INT >= 33 &&
                            !ReminderScheduler.allowed(context)
                    )
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else vm.settings(p.copy(notifications = enabled))
                },
            )
        }
        Text(
            if (ReminderScheduler.allowed(context)) "Android notifications are allowed."
            else "Android notifications are blocked. Items remain available in the app.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            onClick = {
                context.startActivity(
                    android.content
                        .Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        ) {
            Text(stringResource(R.string.ui_android_notification_settings))
        }
        OutlinedTextField(
            warning,
            { warning = it },
            label = { Text(stringResource(R.string.ui_global_warning_window_days)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            reminders,
            { reminders = it },
            label = { Text(stringResource(R.string.ui_default_reminder_offsets)) },
            supportingText = {
                Text(stringResource(R.string.ui_used_for_newly_added_items_0_means_expiry_day))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                runCatching {
                        val n = warning.toInt()
                        require(n in 0..3650) { "Warning days must be 0–3650" }
                        ReminderPolicy.parse(reminders)
                        vm.settings(p.copy(warning = n, reminders = reminders))
                        error = ""
                    }
                    .onFailure { error = it.message ?: "Invalid settings" }
            },
            enabled = !busy,
        ) {
            Text(stringResource(R.string.ui_save_reminder_defaults))
        }
        Choice("Date format", p.dateFormat, listOf("dd MMM yyyy", "dd/MM/yyyy", "yyyy-MM-dd")) {
            vm.settings(p.copy(dateFormat = it))
        }
        Choice(
            "First day of week",
            if (p.mondayFirst) "Monday" else "Sunday",
            listOf("Monday", "Sunday"),
        ) {
            vm.settings(p.copy(mondayFirst = it == "Monday"))
        }
        HorizontalDivider()
        Text(stringResource(R.string.ui_categories), style = MaterialTheme.typography.titleLarge)
        labels
            .filter { it.kind == "category" }
            .forEach { label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label.name, Modifier.weight(1f))
                    IconButton(
                        onClick = { vm.perform("Category removed") { vm.repo.deleteLabel(label) } },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Default.Delete, "Delete ${label.name}")
                    }
                }
            }
        OutlinedTextField(
            category,
            { category = it },
            label = { Text(stringResource(R.string.ui_new_category)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = {
                if (category.isBlank() || category.length > 200)
                    error = "Enter a category up to 200 characters"
                else {
                    val name = category.trim()
                    vm.perform("Category added") { vm.repo.dao.label(Label("category", name)) }
                    category = ""
                }
            },
            enabled = !busy,
        ) {
            Text(stringResource(R.string.ui_add_category))
        }
        Text(
            stringResource(R.string.ui_household_profiles),
            style = MaterialTheme.typography.titleLarge,
        )
        labels
            .filter { it.kind == "profile" }
            .forEach { label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label.name, Modifier.weight(1f))
                    IconButton(
                        onClick = { vm.perform("Profile removed") { vm.repo.deleteLabel(label) } },
                        enabled = !busy,
                    ) {
                        Icon(Icons.Default.Delete, "Delete ${label.name}")
                    }
                }
            }
        OutlinedTextField(
            profile,
            { profile = it },
            label = { Text(stringResource(R.string.ui_new_profile)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = {
                if (profile.isBlank() || profile.length > 200)
                    error = "Enter a profile up to 200 characters"
                else {
                    val name = profile.trim()
                    vm.perform("Profile added") { vm.repo.dao.label(Label("profile", name)) }
                    profile = ""
                }
            },
            enabled = !busy,
        ) {
            Text(stringResource(R.string.ui_add_profile))
        }
        HorizontalDivider()
        Text(
            stringResource(R.string.ui_backup_restore),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Full ZIP backups include images, categories, profiles, settings and history. Keep backups private: they are not encrypted. CSV excludes image files.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = { exportZip.launch("expiry-radar-${LocalDate.now()}.zip") },
            enabled = !busy,
        ) {
            Text(stringResource(R.string.ui_export_backup_zip))
        }
        OutlinedButton(
            onClick = { importZip.launch(arrayOf("application/zip", "application/octet-stream")) },
            enabled = !busy,
        ) {
            Text(stringResource(R.string.ui_restore_backup_zip))
        }
        OutlinedButton(
            onClick = { exportCsv.launch("expiry-radar-${LocalDate.now()}.csv") },
            enabled = !busy,
        ) {
            Text(stringResource(R.string.ui_export_csv))
        }
        OutlinedButton(
            onClick = {
                importCsv.launch(arrayOf("text/*", "application/csv", "application/octet-stream"))
            },
            enabled = !busy,
        ) {
            Text(stringResource(R.string.ui_import_csv))
        }
        Text(
            stringResource(R.string.ui_notification_history),
            style = MaterialTheme.typography.titleLarge,
        )
        if (history.isEmpty()) Text(stringResource(R.string.ui_no_reminders_delivered_yet))
        else
            history.take(30).forEach { entry ->
                Text(
                    "${entry.message}\n${Instant.ofEpochMilli(entry.time).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        HorizontalDivider()
        TextButton(onClick = { dialog = "privacy" }) {
            Text(stringResource(R.string.ui_privacy_policy))
        }
        TextButton(onClick = { dialog = "about" }) {
            Text(stringResource(R.string.ui_about_expiry_radar))
        }
        Text("Version ${com.karthikkilari.expiryradar.BuildConfig.VERSION_NAME}")
        OutlinedButton(onClick = { dialog = "reset" }, enabled = !busy) {
            Text(stringResource(R.string.ui_reset_all_data), color = Red)
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(28.dp))
    }
    when (dialog) {
        "reset" ->
            AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text(stringResource(R.string.ui_delete_all_local_data)) },
                text = {
                    Text(
                        "Items, photos, categories, profiles, preferences and notification history will be removed. Export a backup first. This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog = ""
                            vm.perform("All data reset") {
                                vm.repo.reset()
                                vm.app.preferences.set(Preferences(onboarded = true))
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ui_delete_everything), color = Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = "" }) {
                        Text(stringResource(R.string.ui_cancel))
                    }
                },
            )
        "restore" ->
            AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text("Import $restoreType backup?") },
                text = {
                    Text(
                        "New item IDs will be added. Existing IDs are skipped. ZIP restores preferences too. Files are validated before database changes. CSV does not import images."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val uri = Uri.parse(restoreUri)
                            val type = restoreType
                            dialog = ""
                            vm.perform {
                                val count =
                                    if (type == "ZIP") backup.restore(uri)
                                    else backup.importCsv(uri)
                                vm.messages.send("Imported $count new items")
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ui_import))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = "" }) {
                        Text(stringResource(R.string.ui_cancel))
                    }
                },
            )
        "privacy",
        "about" ->
            AlertDialog(
                onDismissRequest = { dialog = "" },
                title = {
                    Text(if (dialog == "privacy") "Privacy policy" else "About Expiry Radar")
                },
                text = {
                    Box(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            if (dialog == "privacy")
                                "Expiry Radar stores records and compressed photos only on your device. No account, analytics, ads, server or internet permission. Camera access is used only for scanning. Notifications are optional. Selected files are read and written through Android's file picker. Backups are unencrypted and go to your selected destination, which may be a cloud provider. Uninstalling deletes app data unless you have exported a backup. Dates and reminders are organizational aids: verify packaging and never use the app to determine medicine or food safety."
                            else
                                "Track it today. Save it before it expires.\n\nOriginal Kotlin / Compose application. Barcode scanning: ZXing Android Embedded (Apache 2.0). Offline OCR: Google ML Kit bundled Latin model (Google SDK terms). AndroidX components (Apache 2.0). No cloud synchronization. Reminders may be delayed by Android battery restrictions.\n\n" +
                                    remember {
                                        context.assets
                                            .open("third_party_licenses.txt")
                                            .bufferedReader()
                                            .use { it.readText() }
                                    }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { dialog = "" }) {
                        Text(stringResource(R.string.ui_close))
                    }
                },
            )
    }
}
