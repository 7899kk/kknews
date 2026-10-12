@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import java.time.*
import kotlinx.coroutines.*

@Composable
internal fun Items(
    all: List<Item>,
    labels: List<Label>,
    p: Preferences,
    today: LocalDate,
    initial: String,
    open: (Item) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable(initial) { mutableStateOf(initial) }
    var category by rememberSaveable { mutableStateOf("All") }
    var profile by rememberSaveable { mutableStateOf("All") }
    var location by rememberSaveable { mutableStateOf("All") }
    var sort by rememberSaveable { mutableStateOf("Nearest expiry") }
    val list =
        remember(all, search, filter, category, profile, location, sort, today, p.warning) {
            val selected =
                all.filter { i ->
                    (search.isEmpty() ||
                        listOf(i.name, i.barcode, i.notes).any { it.contains(search, true) }) &&
                        (category == "All" || category == i.category) &&
                        (profile == "All" || profile == i.profile) &&
                        (location == "All" || location == i.location) &&
                        when (filter) {
                            "All" -> i.state == "ACTIVE"
                            "Expired" ->
                                i.state == "ACTIVE" && status(i, p, today) == ExpiryStatus.EXPIRED
                            "Expiring Soon" ->
                                i.state == "ACTIVE" &&
                                    status(i, p, today) in
                                        listOf(ExpiryStatus.SOON, ExpiryStatus.TODAY)
                            "Safe" ->
                                i.state == "ACTIVE" && status(i, p, today) == ExpiryStatus.SAFE
                            "Expires Today" ->
                                i.state == "ACTIVE" && status(i, p, today) == ExpiryStatus.TODAY
                            else -> i.state == filter.uppercase()
                        }
                }
            when (sort) {
                "Latest expiry" -> selected.sortedByDescending { it.expiry }
                "Recently added" -> selected.sortedByDescending { it.created }
                "Alphabetical" -> selected.sortedBy { it.name.lowercase() }
                "Category" -> selected.sortedBy { it.category }
                else -> selected.sortedBy { it.expiry }
            }
        }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Heading("Your items", "${list.size} matching items") }
        item {
            OutlinedTextField(
                search,
                { search = it },
                label = { Text(stringResource(R.string.ui_search_name_barcode_or_notes)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Choice(
                    "Status",
                    filter,
                    listOf(
                        "All",
                        "Expiring Soon",
                        "Expires Today",
                        "Expired",
                        "Safe",
                        "Archived",
                        "Used",
                        "Discarded",
                    ),
                ) {
                    filter = it
                }
                Choice(
                    "Category",
                    category,
                    listOf("All") + labels.filter { it.kind == "category" }.map { it.name },
                ) {
                    category = it
                }
                Choice(
                    "Profile",
                    profile,
                    listOf("All") + labels.filter { it.kind == "profile" }.map { it.name },
                ) {
                    profile = it
                }
                Choice(
                    "Location",
                    location,
                    listOf("All") + all.map { it.location }.filter { it.isNotBlank() }.distinct(),
                ) {
                    location = it
                }
                Choice(
                    "Sort",
                    sort,
                    listOf(
                        "Nearest expiry",
                        "Latest expiry",
                        "Recently added",
                        "Alphabetical",
                        "Category",
                    ),
                ) {
                    sort = it
                }
            }
        }
        if (list.isEmpty())
            item { Empty("No matching items", "Try another filter or add an item.") }
        else items(list, key = { it.id }) { i -> ItemCard(i, p, today) { open(i) } }
    }
}

@Composable
internal fun Detail(
    item: Item,
    p: Preferences,
    today: LocalDate,
    back: () -> Unit,
    edit: () -> Unit,
    duplicate: () -> Unit,
    delete: () -> Unit,
    state: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = back) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            Text(stringResource(R.string.ui_back))
        }
        ProductImage(item.image, Modifier.size(120.dp))
        Heading(item.name, "${item.category} · ${item.profile}")
        StatusText(item, p, today)
        listOf(
                "Expiry" to formatted(item.expiry, p),
                "Purchase" to formatted(item.purchase, p),
                "Manufacturing" to formatted(item.manufacturing, p),
                "Quantity" to "${item.quantity} ${item.unit}",
                "Location" to item.location,
                "Barcode" to item.barcode,
                "Reminder days before expiry" to item.reminders.ifEmpty { "None" },
                "Price" to (item.price?.toString() ?: ""),
                "Notes" to item.notes,
                "Completed" to item.completed,
            )
            .filter { it.second.isNotEmpty() }
            .forEach { (label, value) ->
                Text("$label\n$value", style = MaterialTheme.typography.bodyLarge)
            }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = edit) { Text(stringResource(R.string.ui_edit)) }
            OutlinedButton(onClick = duplicate) { Text(stringResource(R.string.ui_duplicate)) }
            TextButton(onClick = delete) { Text(stringResource(R.string.ui_delete), color = Red) }
        }
        if (item.state == "ACTIVE") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { state("USED") }) {
                    Text(stringResource(R.string.ui_mark_used))
                }
                OutlinedButton(onClick = { state("DISCARDED") }) {
                    Text(stringResource(R.string.ui_discarded))
                }
            }
            OutlinedButton(onClick = { state("ARCHIVED") }) {
                Text(stringResource(R.string.ui_archive))
            }
        } else
            Button(onClick = { state("ACTIVE") }) {
                Text(stringResource(R.string.ui_restore_to_active))
            }
    }
}
