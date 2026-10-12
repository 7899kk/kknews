@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val Emerald = Color(0xFF006B50)
internal val Mint = Color(0xFFB7F4D7)
internal val Amber = Color(0xFF956000)
internal val Red = Color(0xFFAD4944)

@Composable
fun RadarApp(vm: RadarViewModel, notificationItem: String?) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val all by vm.items.collectAsStateWithLifecycle()
    val labels by vm.labels.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val dark =
        when (prefs.theme) {
            "Dark" -> true
            "Light" -> false
            else -> isSystemInDarkTheme()
        }
    val colors =
        if (dark)
            darkColorScheme(
                primary = Mint,
                secondary = Color(0xFF80D9B6),
                background = Color(0xFF111B17),
                surface = Color(0xFF17241D),
            )
        else
            lightColorScheme(
                primary = Emerald,
                secondary = Color(0xFF386A54),
                background = Color(0xFFFAFAF3),
                surface = Color(0xFFFAFAF3),
                primaryContainer = Mint,
            )
    MaterialTheme(colorScheme = colors) {
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { for (message in vm.messages) snackbar.showSnackbar(message) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var route by rememberSaveable { mutableStateOf("main") }
        var editJson by rememberSaveable { mutableStateOf("") }
        var detailId by rememberSaveable { mutableStateOf(notificationItem ?: "") }
        var initialScan by rememberSaveable { mutableStateOf("") }
        var initialStatus by rememberSaveable { mutableStateOf("All") }
        var deleteId by rememberSaveable { mutableStateOf("") }
        fun edit(item: Item? = null, scan: String = "") {
            editJson = Json.encodeToString(item ?: Item(expiry = "", reminders = prefs.reminders))
            initialScan = scan
            route = "edit"
        }
        fun detail(item: Item) {
            detailId = item.id
            route = "detail"
        }
        LaunchedEffect(notificationItem) { if (notificationItem != null) route = "detail" }
        BackHandler(route != "main") { route = "main" }
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (loaded && prefs.onboarded && route == "main")
                    NavigationBar {
                        listOf(
                                stringResource(R.string.home) to Icons.Default.Home,
                                stringResource(R.string.items) to RadarIcons.Inventory,
                                stringResource(R.string.calendar) to RadarIcons.Calendar,
                                stringResource(R.string.insights) to RadarIcons.Chart,
                                stringResource(R.string.settings) to Icons.Default.Settings,
                            )
                            .forEachIndexed { i, (name, icon) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = {
                                        tab = i
                                        initialStatus = "All"
                                    },
                                    icon = { Icon(icon, name) },
                                    label = { Text(name, maxLines = 1) },
                                )
                            }
                    }
            },
            floatingActionButton = {
                if (loaded && prefs.onboarded && route == "main" && tab < 3)
                    FloatingActionButton(onClick = { edit() }) {
                        Icon(Icons.Default.Add, "Add item")
                    }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    !loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    !prefs.onboarded -> Onboarding { vm.settings(prefs.copy(onboarded = true)) }
                    route == "edit" ->
                        Editor(
                            vm,
                            editJson,
                            { editJson = it },
                            initialScan,
                            { initialScan = "" },
                            { route = "main" },
                        )
                    route == "detail" -> {
                        val item = all.find { it.id == detailId }
                        if (item == null)
                            Column(Modifier.padding(24.dp)) {
                                Text(stringResource(R.string.ui_item_unavailable))
                                TextButton(onClick = { route = "main" }) {
                                    Text(stringResource(R.string.ui_back))
                                }
                            }
                        else
                            Detail(
                                item,
                                prefs,
                                today,
                                { route = "main" },
                                { edit(item) },
                                {
                                    edit(
                                        item.copy(
                                            id = UUID.randomUUID().toString(),
                                            created = System.currentTimeMillis(),
                                            state = "ACTIVE",
                                            completed = "",
                                        )
                                    )
                                },
                                { deleteId = item.id },
                                { state ->
                                    vm.perform("Item updated") { vm.repo.state(item, state) }
                                },
                            )
                    }
                    else ->
                        when (tab) {
                            0 ->
                                Home(
                                    all,
                                    prefs,
                                    today,
                                    { detail(it) },
                                    { edit() },
                                    { edit(scan = "barcode") },
                                    { tab = 2 },
                                    {
                                        initialStatus = "Expired"
                                        tab = 1
                                    },
                                )
                            1 -> Items(all, labels, prefs, today, initialStatus, { detail(it) })
                            2 -> CalendarScreen(all, labels, prefs, today, { detail(it) })
                            3 -> Insights(all, prefs, today)
                            4 -> Settings(vm, prefs, labels, history, busy)
                        }
                }
                if (busy)
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
        if (deleteId.isNotEmpty())
            AlertDialog(
                onDismissRequest = { deleteId = "" },
                title = { Text(stringResource(R.string.ui_delete_this_item)) },
                text = {
                    Text(
                        stringResource(R.string.ui_this_removes_the_item_and_cancels_its_reminders)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = deleteId
                            deleteId = ""
                            vm.perform("Item deleted") {
                                all.find { it.id == id }?.let { vm.repo.delete(it) }
                                route = "main"
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ui_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteId = "" }) {
                        Text(stringResource(R.string.ui_cancel))
                    }
                },
            )
    }
}
