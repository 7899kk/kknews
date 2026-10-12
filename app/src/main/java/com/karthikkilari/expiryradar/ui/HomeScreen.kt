@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import java.time.*
import kotlinx.coroutines.*

@Composable
internal fun Home(
    all: List<Item>,
    p: Preferences,
    today: LocalDate,
    open: (Item) -> Unit,
    add: () -> Unit,
    scan: () -> Unit,
    calendar: () -> Unit,
    expired: () -> Unit,
) {
    val items = active(all)
    val hour = LocalTime.now().hour
    val soon =
        items.filter { status(it, p, today) in listOf(ExpiryStatus.SOON, ExpiryStatus.TODAY) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Heading(
                "Good ${if(hour<12)"Morning" else if(hour<17)"Afternoon" else "Evening"} 👋",
                "Here's what's happening with your items.",
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("Total Items", items.size, Modifier.weight(1f))
                    Stat("Expiring Soon", soon.size, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat(
                        "Expired",
                        items.count { status(it, p, today) == ExpiryStatus.EXPIRED },
                        Modifier.weight(1f),
                    )
                    Stat(
                        "Safe",
                        items.count { status(it, p, today) == ExpiryStatus.SAFE },
                        Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = add) { Text(stringResource(R.string.ui_add_item)) }
                OutlinedButton(onClick = scan) { Text(stringResource(R.string.ui_scan_barcode_2)) }
                OutlinedButton(onClick = calendar) { Text(stringResource(R.string.ui_calendar)) }
                OutlinedButton(onClick = expired) { Text(stringResource(R.string.ui_expired)) }
            }
        }
        if (items.isEmpty())
            item {
                Empty(
                    "Nothing to track yet.",
                    "Add your first item and let Expiry Radar remember the dates for you.",
                )
                Button(onClick = add) { Text(stringResource(R.string.ui_add_first_item)) }
            }
        else {
            item {
                Text(
                    stringResource(R.string.ui_expiring_soon),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (soon.isEmpty()) item { Empty("All clear", "No items inside your warning window.") }
            items(soon.sortedBy { it.expiry }.take(20), key = { it.id }) {
                ItemCard(it, p, today) { open(it) }
            }
        }
    }
}
