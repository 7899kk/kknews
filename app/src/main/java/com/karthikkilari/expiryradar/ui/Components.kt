@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*

@Composable
internal fun Heading(title: String, subtitle: String = "") {
    Column(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle.isNotEmpty())
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

internal fun active(items: List<Item>) = items.filter { it.state == "ACTIVE" }

internal fun status(item: Item, p: Preferences, today: LocalDate) =
    Expiry.status(
        LocalDate.parse(item.expiry),
        today,
        if (item.warning < 0) p.warning else item.warning,
    )

internal fun formatted(date: String, p: Preferences) =
    runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern(p.dateFormat)) }
        .getOrDefault(date)

@Composable
internal fun StatusText(item: Item, p: Preferences, today: LocalDate) {
    val s = status(item, p, today)
    Text(
        if (item.state != "ACTIVE") item.state.lowercase().replaceFirstChar { it.uppercase() }
        else Expiry.label(LocalDate.parse(item.expiry), today),
        color =
            when (s) {
                ExpiryStatus.EXPIRED -> Red
                ExpiryStatus.SOON,
                ExpiryStatus.TODAY -> Amber
                else -> MaterialTheme.colorScheme.primary
            },
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
internal fun ItemCard(item: Item, p: Preferences, today: LocalDate, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(item.image, Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${item.category} · ${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(formatted(item.expiry, p), style = MaterialTheme.typography.bodySmall)
                StatusText(item, p, today)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open item")
        }
    }
}

@Composable
internal fun ProductImage(name: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by
        produceState<android.graphics.Bitmap?>(null, name) {
            value =
                withContext(Dispatchers.IO) {
                    if (name.isEmpty()) null
                    else
                        android.graphics.BitmapFactory.decodeFile(
                            File(File(context.filesDir, "images"), name).path,
                            android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 },
                        )
                }
        }
    val image = bitmap
    if (image == null)
        Icon(
            RadarIcons.Inventory,
            "Product",
            modifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    else Image(image.asImageBitmap(), "Product image", modifier)
}

@Composable
internal fun Stat(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun Empty(title: String, text: String) {
    Column(Modifier.padding(vertical = 28.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun Choice(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) {
            Text("$label: $value")
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onChange(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun DateField(
    label: String,
    value: String,
    optional: Boolean,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Icon(RadarIcons.Calendar, null)
            Text("$label: ${value.ifEmpty {"Not set"}}")
        }
        if (optional && value.isNotEmpty())
            IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Clear, "Clear $label") }
    }
    if (open) {
        val selected =
            runCatching {
                    LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                }
                .getOrNull()
        val picker =
            rememberDatePickerState(initialSelectedDateMillis = selected, yearRange = 1900..2199)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let {
                            onChange(
                                Instant.ofEpochMilli(it)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString()
                            )
                        }
                        open = false
                    }
                ) {
                    Text(stringResource(R.string.ui_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.ui_cancel)) }
            },
        ) {
            DatePicker(picker)
        }
    }
}

@Composable
internal fun DateFieldDialog(value: String, confirm: (String) -> Unit, dismiss: () -> Unit) {
    val picker =
        rememberDatePickerState(
            initialSelectedDateMillis =
                LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1900..2199,
        )
    DatePickerDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    picker.selectedDateMillis?.let {
                        confirm(
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.ui_go))
            }
        },
        dismissButton = {
            TextButton(onClick = dismiss) { Text(stringResource(R.string.ui_cancel)) }
        },
    ) {
        DatePicker(picker)
    }
}

@Composable
internal fun Bar(label: String, count: Int, total: Int) {
    Column {
        Text("$label · $count")
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else count.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}
