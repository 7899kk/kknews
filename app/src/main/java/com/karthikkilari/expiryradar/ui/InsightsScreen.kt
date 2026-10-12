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
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

@Composable
internal fun Insights(all: List<Item>, p: Preferences, today: LocalDate) {
    val used = all.filter { it.state == "USED" }
    val discarded = all.filter { it.state == "DISCARDED" }
    val categories =
        all.groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Heading("Your insights", "Measured from your local records.") }
        item { Stat("Total tracked records", all.size, Modifier.fillMaxWidth()) }
        item {
            Stat(
                "Used on or before expiry",
                used.count { it.completed.isNotEmpty() && it.completed <= it.expiry },
                Modifier.fillMaxWidth(),
            )
        }
        item {
            Stat("Discarded", discarded.size, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Stat(
                "Expired active items",
                active(all).count { status(it, p, today) == ExpiryStatus.EXPIRED },
                Modifier.fillMaxWidth(),
            )
        }
        item {
            Text("Recorded discarded value: ₹${"%.2f".format(discarded.sumOf {it.price?:0.0})}")
            Text(
                "Includes only prices entered on discarded records. This is not a savings estimate.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Text(
                stringResource(R.string.ui_category_distribution),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(categories) { (name, count) -> Bar(name, count, all.size) }
        item {
            Text(
                stringResource(R.string.ui_monthly_expiries),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items((0L..5L).map { YearMonth.from(today).plusMonths(it) }) { month ->
            Bar(
                month.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                active(all).count { it.expiry.startsWith(month.toString()) },
                active(all).size,
            )
        }
        item {
            Text(
                stringResource(R.string.ui_most_discarded_categories),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(
            discarded
                .groupingBy { it.category }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
        ) { (name, count) ->
            Bar(name, count, discarded.size)
        }
        item {
            Text(
                stringResource(R.string.ui_usage_history),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (used.isEmpty()) item { Text(stringResource(R.string.ui_no_items_marked_used_yet)) }
        else
            items(used.sortedByDescending { it.completed }.take(100), key = { it.id }) {
                Text("${it.name} · ${formatted(it.completed,p)}")
            }
    }
}
