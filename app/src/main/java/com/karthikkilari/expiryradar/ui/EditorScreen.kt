@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import com.karthikkilari.expiryradar.workers.ReminderScheduler
import java.io.File
import java.time.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
internal fun Editor(
    vm: RadarViewModel,
    encoded: String,
    update: (String) -> Unit,
    initialScan: String,
    clearScan: () -> Unit,
    back: () -> Unit,
) {
    val item = remember(encoded) { Json.decodeFromString<Item>(encoded) }
    fun change(value: Item) {
        update(Json.encodeToString(value))
    }
    val p by vm.prefs.collectAsStateWithLifecycle()
    val labels by vm.labels.collectAsStateWithLifecycle()
    val all by vm.items.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var quantity by rememberSaveable(item.id) { mutableStateOf(item.quantity.toString()) }
    var price by rememberSaveable(item.id) { mutableStateOf(item.price?.toString() ?: "") }
    var warning by
        rememberSaveable(item.id) {
            mutableStateOf(if (item.warning < 0) "" else item.warning.toString())
        }
    var error by rememberSaveable { mutableStateOf("") }
    var confirmSave by remember { mutableStateOf(false) }
    var barcodeResult by rememberSaveable { mutableStateOf("") }
    var scanUri by rememberSaveable { mutableStateOf("") }
    var pendingCamera by rememberSaveable { mutableStateOf("") }
    var ocrBusy by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<DateCandidate>?>(null) }
    var ocrText by remember { mutableStateOf("") }
    val scanner =
        rememberLauncherForActivityResult(ScanContract()) { result ->
            if (result.contents != null) barcodeResult = result.contents
            else error = "Scan cancelled. You can enter the barcode manually."
        }
    val capture =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && scanUri.isNotEmpty()) {
                ocrBusy = true
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    recognizer
                        .process(InputImage.fromFilePath(context, Uri.parse(scanUri)))
                        .addOnSuccessListener { result ->
                            ocrText = result.text
                            candidates = DateParser.candidates(result.text)
                            ocrBusy = false
                            recognizer.close()
                        }
                        .addOnFailureListener {
                            error = it.message ?: "Could not recognize text"
                            ocrBusy = false
                            recognizer.close()
                        }
                } catch (e: Exception) {
                    error = e.message ?: "Could not open image"
                    ocrBusy = false
                    recognizer.close()
                }
            } else error = "Capture cancelled. Use the date picker instead."
        }
    fun launchCamera(kind: String) {
        error = ""
        if (kind == "barcode")
            scanner.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                    .setPrompt("Scan a product barcode")
                    .setBeepEnabled(false)
                    .setOrientationLocked(false)
            )
        else {
            val file = File(File(context.cacheDir, "capture").apply { mkdirs() }, "expiry.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            scanUri = uri.toString()
            runCatching { capture.launch(uri) }
                .onFailure { error = "No camera app available. Enter the expiry manually." }
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera(pendingCamera)
            else error = "Camera permission denied. Manual entry is available."
        }
    fun requestCamera(kind: String) {
        pendingCamera = kind
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
            launchCamera(kind)
        else permission.launch(Manifest.permission.CAMERA)
    }
    val notifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) error = "Notifications are disabled. Your item will still be saved."
            else ReminderScheduler.reconcile(context)
        }
    val photo =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null)
                scope.launch {
                    runCatching { vm.repo.importImage(uri) }
                        .onSuccess { change(item.copy(image = it)) }
                        .onFailure { error = it.message ?: "Image could not be imported" }
                }
        }
    LaunchedEffect(initialScan) {
        if (initialScan == "barcode") {
            clearScan()
            requestCamera("barcode")
        }
    }
    fun save() {
        val actual =
            item.copy(
                quantity = quantity.toDoubleOrNull() ?: Double.NaN,
                price = if (price.isBlank()) null else price.toDoubleOrNull() ?: Double.NaN,
                warning = if (warning.isBlank()) -1 else warning.toIntOrNull() ?: -2,
            )
        runCatching { vm.repo.validate(actual) }
            .onFailure { error = it.message ?: "Check your inputs" }
            .onSuccess {
                vm.perform("Item saved") {
                    vm.repo.save(actual)
                    withContext(Dispatchers.Main) { back() }
                }
            }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = back, enabled = !busy) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            Text(stringResource(R.string.ui_cancel))
        }
        Heading(
            if (all.any { it.id == item.id }) "Edit item" else "Add an item",
            "A batch has its own expiry date.",
        )
        OutlinedTextField(
            item.name,
            { change(item.copy(name = it)) },
            label = { Text(stringResource(R.string.ui_product_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Choice(
            "Category",
            item.category,
            labels.filter { it.kind == "category" }.map { it.name }.ifEmpty { defaultCategories },
        ) {
            change(item.copy(category = it))
        }
        Choice(
            "Profile",
            item.profile,
            labels.filter { it.kind == "profile" }.map { it.name }.ifEmpty { listOf("My Items") },
        ) {
            change(item.copy(profile = it))
        }
        DateField("Expiry date *", item.expiry, false) { change(item.copy(expiry = it)) }
        OutlinedButton(onClick = { requestCamera("ocr") }, enabled = !ocrBusy) {
            Icon(RadarIcons.Scanner, null)
            Text(if (ocrBusy) "Recognizing text…" else "Scan expiry date (offline)")
        }
        DateField("Purchase date", item.purchase, true) { change(item.copy(purchase = it)) }
        DateField("Manufacturing date", item.manufacturing, true) {
            change(item.copy(manufacturing = it))
        }
        OutlinedTextField(
            quantity,
            { quantity = it },
            label = { Text(stringResource(R.string.ui_quantity)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Choice(
            "Unit",
            item.unit,
            listOf(
                "Pieces",
                "Packets",
                "Bottles",
                "Litres",
                "Millilitres",
                "Kilograms",
                "Grams",
                "Boxes",
                "Custom",
            ),
        ) {
            change(item.copy(unit = it))
        }
        if (
            item.unit == "Custom" ||
                item.unit !in
                    listOf(
                        "Pieces",
                        "Packets",
                        "Bottles",
                        "Litres",
                        "Millilitres",
                        "Kilograms",
                        "Grams",
                        "Boxes",
                    )
        )
            OutlinedTextField(
                item.unit,
                { change(item.copy(unit = it)) },
                label = { Text(stringResource(R.string.ui_custom_unit)) },
                modifier = Modifier.fillMaxWidth(),
            )
        OutlinedTextField(
            item.location,
            { change(item.copy(location = it)) },
            label = { Text(stringResource(R.string.ui_storage_location)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductImage(item.image, Modifier.size(60.dp))
            OutlinedButton(
                onClick = {
                    photo.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.ui_add_image))
            }
            if (item.image.isNotEmpty())
                TextButton(onClick = { change(item.copy(image = "")) }) {
                    Text(stringResource(R.string.ui_remove))
                }
        }
        OutlinedTextField(
            item.barcode,
            { change(item.copy(barcode = it)) },
            label = { Text(stringResource(R.string.ui_barcode)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { requestCamera("barcode") }) {
            Text(stringResource(R.string.ui_scan_barcode))
        }
        Text(
            "Barcodes do not normally contain expiry dates.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            price,
            { price = it },
            label = { Text(stringResource(R.string.ui_optional_purchase_price)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            item.notes,
            { change(item.copy(notes = it)) },
            label = { Text(stringResource(R.string.ui_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Text(stringResource(R.string.ui_reminders), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            item.reminders,
            { change(item.copy(reminders = it)) },
            label = { Text(stringResource(R.string.ui_days_before_expiry_separated_by_commas)) },
            supportingText = {
                Text(
                    stringResource(R.string.ui_examples_30_14_7_3_1_0_leave_empty_for_no_reminders)
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (item.reminders.isNotEmpty() && !ReminderScheduler.allowed(context))
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33)
                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else {
                        context.startActivity(
                            android.content
                                .Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(
                                    android.provider.Settings.EXTRA_APP_PACKAGE,
                                    context.packageName,
                                )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.ui_enable_notification_permission))
            }
        OutlinedTextField(
            warning,
            { warning = it },
            label = { Text("Warning window (blank uses global ${p.warning} days)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = {
                val e = runCatching { LocalDate.parse(item.expiry) }.getOrNull()
                val m = runCatching { LocalDate.parse(item.manufacturing) }.getOrNull()
                val purchase = runCatching { LocalDate.parse(item.purchase) }.getOrNull()
                if (
                    e != null &&
                        (e < LocalDate.now() ||
                            (m != null && m > e) ||
                            (purchase != null && purchase > e) ||
                            (m != null && purchase != null && m > purchase))
                )
                    confirmSave = true
                else save()
            },
            enabled = !busy && !ocrBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Saving…" else "Save Item")
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmSave)
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text(stringResource(R.string.ui_check_the_dates)) },
            text = {
                Text(
                    "The expiry is in the past, or the dates are out of order. Save this record anyway?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSave = false
                        save()
                    }
                ) {
                    Text(stringResource(R.string.ui_save_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSave = false }) {
                    Text(stringResource(R.string.ui_review))
                }
            },
        )
    if (barcodeResult.isNotEmpty())
        AlertDialog(
            onDismissRequest = { barcodeResult = "" },
            title = { Text(stringResource(R.string.ui_barcode_recognized)) },
            text = {
                Column {
                    Text(barcodeResult)
                    Text(stringResource(R.string.ui_expiry_must_be_entered_separately))
                    all.filter { it.barcode == barcodeResult }
                        .take(5)
                        .forEach { local ->
                            TextButton(
                                onClick = {
                                    change(
                                        item.copy(
                                            barcode = barcodeResult,
                                            name = local.name,
                                            category = local.category,
                                            unit = local.unit,
                                            location = local.location,
                                        )
                                    )
                                    barcodeResult = ""
                                }
                            ) {
                                Text("Reuse details: ${local.name}")
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        change(item.copy(barcode = barcodeResult))
                        barcodeResult = ""
                    }
                ) {
                    Text(stringResource(R.string.ui_use_barcode))
                }
            },
            dismissButton = {
                TextButton(onClick = { barcodeResult = "" }) {
                    Text(stringResource(R.string.ui_cancel))
                }
            },
        )
    val options = candidates
    if (options != null)
        AlertDialog(
            onDismissRequest = { candidates = null },
            title = { Text(stringResource(R.string.ui_confirm_a_printed_date)) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        "Select only the intended expiry. MFG is not an expiry date. Month/year dates use the month's last day; verify the package instructions."
                    )
                    if (options.isEmpty())
                        Text(
                            stringResource(
                                R.string.ui_no_supported_dates_found_use_the_manual_picker
                            )
                        )
                    options.forEach { candidate ->
                        TextButton(
                            onClick = {
                                change(item.copy(expiry = candidate.date.toString()))
                                candidates = null
                            }
                        ) {
                            Text(
                                "${candidate.date} · ${candidate.context}${if(candidate.ambiguous)" · ambiguous format" else ""}"
                            )
                        }
                    }
                    Text(ocrText.take(2000), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { candidates = null }) {
                    Text(stringResource(R.string.ui_close))
                }
            },
        )
}
