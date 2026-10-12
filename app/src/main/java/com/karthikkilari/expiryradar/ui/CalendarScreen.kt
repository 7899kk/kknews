@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

@Composable
internal fun CalendarScreen(
    all: List<Item>,
    labels: List<Label>,
    p: Preferences,
    today: LocalDate,
    open: (Item) -> Unit,
) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthText)
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val selected = LocalDate.parse(selectedText)
    var profile by rememberSaveable { mutableStateOf("All") }
    var category by rememberSaveable { mutableStateOf("All") }
    var pickMonth by remember { mutableStateOf(false) }
    val list =
        active(all).filter {
            (profile == "All" || it.profile == profile) &&
                (category == "All" || it.category == category)
        }
    val counts = list.groupingBy { it.expiry }.eachCount()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Heading("Expiry calendar", "A clear view of the month ahead.") }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Choice(
                    "Profile",
                    profile,
                    listOf("All") + labels.filter { it.kind == "profile" }.map { it.name },
                ) {
                    profile = it
                }
                Choice(
                    "Category",
                    category,
                    listOf("All") + labels.filter { it.kind == "category" }.map { it.name },
                ) {
                    category = it
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                }
                TextButton(onClick = { pickMonth = true }, modifier = Modifier.weight(1f)) {
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
                }
                IconButton(onClick = { monthText = month.plusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                }
            }
            TextButton(
                onClick = {
                    monthText = YearMonth.from(today).toString()
                    selectedText = today.toString()
                }
            ) {
                Text(stringResource(R.string.ui_jump_to_today))
            }
        }
        item {
            Column {
                val days =
                    if (p.mondayFirst) listOf("M", "T", "W", "T", "F", "S", "S")
                    else listOf("S", "M", "T", "W", "T", "F", "S")
                Row { days.forEach { Text(it, Modifier.weight(1f).padding(8.dp)) } }
                val offset =
                    if (p.mondayFirst) month.atDay(1).dayOfWeek.value - 1
                    else month.atDay(1).dayOfWeek.value % 7
                val cells = List(offset) { 0 } + (1..month.lengthOfMonth()).toList()
                cells.chunked(7).forEach { week ->
                    Row {
                        (week + List(7 - week.size) { 0 }).forEach { day ->
                            val date = if (day > 0) month.atDay(day) else null
                            val n = counts[date?.toString()] ?: 0
                            Box(
                                Modifier.weight(1f)
                                    .heightIn(min = 52.dp)
                                    .padding(2.dp)
                                    .background(
                                        if (date == selected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .then(
                                        if (date == null) Modifier
                                        else Modifier.clickable { selectedText = date.toString() }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (day > 0)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(day.toString())
                                        if (n > 0)
                                            Text(
                                                "• $n",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                    }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Expiring ${formatted(selectedText,p)}",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        val onDay = list.filter { it.expiry == selectedText }
        if (onDay.isEmpty())
            item { Empty("No expiries on this day", "Select a highlighted day to see its items.") }
        else items(onDay, key = { it.id }) { ItemCard(it, p, today) { open(it) } }
    }
    if (pickMonth)
        DateFieldDialog(
            selectedText,
            { date ->
                monthText = YearMonth.from(LocalDate.parse(date)).toString()
                selectedText = date
                pickMonth = false
            },
            { pickMonth = false },
        )
}
