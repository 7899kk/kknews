package com.karthikkilari.expiryradar.backup

import android.net.Uri
import androidx.room.withTransaction
import com.karthikkilari.expiryradar.RadarApplication
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.workers.ReminderScheduler
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable
data class Snapshot(
    val version: Int = 1,
    val items: List<Item>,
    val labels: List<Label>,
    val history: List<History>,
    val preferences: Preferences,
)

@Serializable data class Integrity(val version: Int = 1, val sha256: Map<String, String>)

class BackupManager(private val app: RadarApplication) {
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }
    private val repo = app.repository

    private fun hash(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    suspend fun export(uri: Uri): Unit =
        withContext(Dispatchers.IO) {
            val snapshot =
                app.database.withTransaction {
                    Snapshot(
                        items = repo.dao.all(),
                        labels = repo.dao.allLabels(),
                        history = repo.dao.allHistory(),
                        preferences = app.preferences.flow.first(),
                    )
                }
            require(snapshot.items.size <= 20000) { "A backup supports up to 20,000 items" }
            val data = json.encodeToString(snapshot).toByteArray()
            require(data.size <= 32 * 1024 * 1024) { "Backup metadata is too large" }
            val imageFiles =
                snapshot.items
                    .map { it.image }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .map { name ->
                        require(Regex("[a-zA-Z0-9-]+\\.jpg").matches(name))
                        val file = File(File(app.filesDir, "images"), name)
                        require(file.exists() && file.length() <= 32L * 1024 * 1024) {
                            "Missing or oversized product image"
                        }
                        file
                    }
            require(data.size + imageFiles.sumOf { it.length() } <= 252L * 1024 * 1024) {
                "Backup exceeds the 256 MiB restoration limit"
            }
            val checks = linkedMapOf<String, String>()
            app.contentResolver.openOutputStream(uri, "wt").use { stream ->
                ZipOutputStream(requireNotNull(stream)).use { zip ->
                    fun entry(name: String, bytes: ByteArray) {
                        checks[name] = hash(bytes)
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                    entry("data.json", data)
                    snapshot.items
                        .map { it.image }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .forEach { name ->
                            require(Regex("[a-zA-Z0-9-]+\\.jpg").matches(name))
                            val file = File(File(app.filesDir, "images"), name)
                            require(file.exists()) { "Missing product image: $name" }
                            entry("images/$name", file.readBytes())
                        }
                    zip.putNextEntry(ZipEntry("integrity.json"))
                    zip.write(json.encodeToString(Integrity(sha256 = checks)).toByteArray())
                    zip.closeEntry()
                }
            }
        }

    suspend fun restore(uri: Uri): Int =
        withContext(Dispatchers.IO) {
            val entries = linkedMapOf<String, ByteArray>()
            var total = 0L
            app.contentResolver.openInputStream(uri).use { stream ->
                ZipInputStream(requireNotNull(stream)).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        require(
                            entries.size < 20005 &&
                                !entry.isDirectory &&
                                (entry.name in listOf("data.json", "integrity.json") ||
                                    Regex("images/[a-zA-Z0-9-]+\\.jpg").matches(entry.name))
                        ) {
                            "Unsupported ZIP entry"
                        }
                        require(!entries.containsKey(entry.name)) { "Duplicate ZIP entry" }
                        val buffer = ByteArrayOutputStream()
                        val block = ByteArray(8192)
                        var size = 0
                        while (true) {
                            val n = zip.read(block)
                            if (n < 0) break
                            size += n
                            total += n
                            require(size <= 32 * 1024 * 1024 && total <= 256L * 1024 * 1024) {
                                "Backup is too large"
                            }
                            buffer.write(block, 0, n)
                        }
                        entries[entry.name] = buffer.toByteArray()
                    }
                }
            }
            val integrity =
                json.decodeFromString<Integrity>(
                    (entries.remove("integrity.json") ?: error("Missing integrity manifest"))
                        .decodeToString()
                )
            require(integrity.version == 1 && integrity.sha256.keys == entries.keys) {
                "Unsupported or incomplete backup"
            }
            entries.forEach { (name, bytes) ->
                require(integrity.sha256[name] == hash(bytes)) { "Backup failed integrity check" }
            }
            val snapshot =
                json.decodeFromString<Snapshot>(
                    (entries["data.json"] ?: error("Missing data")).decodeToString()
                )
            require(snapshot.version == 1) { "Unsupported backup version" }
            require(
                snapshot.items.size <= 20000 &&
                    snapshot.items.map { it.id }.distinct().size == snapshot.items.size
            ) {
                "Invalid item count or IDs"
            }
            snapshot.items.forEach {
                repo.validate(it)
                require(it.id.length in 1..100)
                if (it.image.isNotEmpty())
                    require(entries.containsKey("images/${it.image}")) { "Missing image" }
            }
            require(
                snapshot.preferences.theme in listOf("System", "Light", "Dark") &&
                    snapshot.preferences.warning in 0..3650 &&
                    snapshot.preferences.dateFormat in
                        listOf("dd MMM yyyy", "dd/MM/yyyy", "yyyy-MM-dd")
            ) {
                "Invalid preferences"
            }
            com.karthikkilari.expiryradar.domain.ReminderPolicy.parse(
                snapshot.preferences.reminders
            )
            require(
                snapshot.labels.all {
                    it.kind in listOf("category", "profile") &&
                        it.name.isNotBlank() &&
                        it.name.length <= 200
                }
            )
            val existing = repo.dao.all().map { it.id }.toSet()
            val incoming = snapshot.items.filter { it.id !in existing }
            val copied = mutableListOf<File>()
            val names = mutableMapOf<String, String>()
            try {
                val images = File(app.filesDir, "images").apply { mkdirs() }
                incoming
                    .filter { it.image.isNotEmpty() }
                    .forEach { item ->
                        if (item.image !in names) {
                            val name = "${UUID.randomUUID()}.jpg"
                            val dest = File(images, name)
                            dest.writeBytes(requireNotNull(entries["images/${item.image}"]))
                            copied.add(dest)
                            names[item.image] = name
                        }
                    }
                app.database.withTransaction {
                    repo.dao.saveAll(
                        incoming.map {
                            if (it.image.isEmpty()) it
                            else it.copy(image = names.getValue(it.image))
                        }
                    )
                    repo.dao.labels(snapshot.labels)
                    repo.dao.histories(snapshot.history)
                }
            } catch (e: Exception) {
                copied.forEach { it.delete() }
                throw e
            }
            app.preferences.set(snapshot.preferences.copy(onboarded = true))
            repo.seed()
            ReminderScheduler.reconcile(app)
            incoming.size
        }

    suspend fun exportCsv(uri: Uri): Unit =
        withContext(Dispatchers.IO) {
            val rows = repo.dao.all().map { item -> json.encodeToJsonElement(item).jsonObject }
            val header = json.encodeToJsonElement(Item()).jsonObject.keys.toList()
            val csv =
                Csv.encode(
                    listOf(header) +
                        rows.map { row ->
                            header.map { key -> (row[key] as? JsonPrimitive)?.contentOrNull ?: "" }
                        }
                )
            app.contentResolver.openOutputStream(uri, "wt").use {
                requireNotNull(it).write(csv.toByteArray())
            }
        }

    suspend fun importCsv(uri: Uri): Int =
        withContext(Dispatchers.IO) {
            val bytes =
                app.contentResolver.openInputStream(uri).use { source ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    val input = requireNotNull(source)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        require(out.size() + n <= 16 * 1024 * 1024) { "CSV is too large" }
                        out.write(buffer, 0, n)
                    }
                    out.toByteArray()
                }
            require(bytes.size <= 16 * 1024 * 1024) { "CSV is too large" }
            val rows = Csv.decode(bytes.decodeToString().removePrefix("\uFEFF"))
            require(rows.isNotEmpty())
            val header = rows.first()
            require(
                header.containsAll(listOf("id", "name", "expiry")) &&
                    header.distinct().size == header.size
            ) {
                "Unsupported CSV header"
            }
            val numeric = setOf("quantity", "warning", "created", "updated", "price")
            val items =
                rows
                    .drop(1)
                    .filter { it.any(String::isNotBlank) }
                    .map { row ->
                        require(row.size == header.size) { "Invalid CSV column count" }
                        val obj =
                            JsonObject(
                                header.zip(row).associate { (k, v) ->
                                    k to
                                        if (k in numeric)
                                            (if (v.isEmpty() && k == "price") JsonNull
                                            else
                                                JsonPrimitive(
                                                    v.toDoubleOrNull()?.let {
                                                        if (
                                                            k in
                                                                setOf(
                                                                    "warning",
                                                                    "created",
                                                                    "updated",
                                                                )
                                                        )
                                                            v.toLong()
                                                        else it
                                                    } ?: error("Invalid number")
                                                ))
                                        else JsonPrimitive(v)
                                }
                            )
                        json.decodeFromJsonElement<Item>(obj).copy(image = "")
                    }
            require(items.size <= 20000 && items.map { it.id }.distinct().size == items.size)
            items.forEach(repo::validate)
            val ids = repo.dao.all().map { it.id }.toSet()
            val incoming = items.filter { it.id !in ids }
            app.database.withTransaction {
                repo.dao.saveAll(incoming)
                repo.dao.labels(
                    incoming.flatMap {
                        listOf(Label("category", it.category), Label("profile", it.profile))
                    }
                )
            }
            ReminderScheduler.reconcile(app)
            incoming.size
        }
}
