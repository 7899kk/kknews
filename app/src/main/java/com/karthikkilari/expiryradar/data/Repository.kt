package com.karthikkilari.expiryradar.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.withTransaction
import com.karthikkilari.expiryradar.domain.ReminderPolicy
import com.karthikkilari.expiryradar.workers.ReminderScheduler
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Repository(val context: Context, val database: RadarDatabase) {
    val dao = database.dao()
    val items = dao.observe()
    val labels = dao.labels()
    val history = dao.history()

    suspend fun seed() {
        val current = dao.allLabels()
        if (current.none { it.kind == "category" })
            dao.labels(defaultCategories.map { Label("category", it) })
        if (current.none { it.kind == "profile" }) dao.label(Label("profile", "My Items"))
    }

    fun validate(item: Item) {
        require(Regex("[a-zA-Z0-9_-]{1,100}").matches(item.id)) { "Invalid item identifier" }
        require(item.image.isEmpty() || Regex("[a-zA-Z0-9-]+\\.jpg").matches(item.image)) {
            "Invalid image name"
        }
        require(
            item.notes.length <= 20000 && item.barcode.length <= 1000 && item.location.length <= 500
        ) {
            "Item text is too long"
        }
        require(item.name.isNotBlank()) { "Enter a product name" }
        require(item.name.length <= 200) { "Name is too long" }
        require(item.expiry.isNotBlank()) { "Choose an expiry date" }
        listOf(item.expiry, item.purchase, item.manufacturing, item.completed)
            .filter { it.isNotEmpty() }
            .forEach { value ->
                require(LocalDate.parse(value).year in 1900..2199) {
                    "Dates must be between 1900 and 2199"
                }
            }
        require(item.quantity.isFinite() && item.quantity > 0) {
            "Quantity must be greater than zero"
        }
        require(item.price == null || (item.price.isFinite() && item.price >= 0)) {
            "Enter a valid price"
        }
        require(item.warning in -1..3650) { "Invalid warning period" }
        ReminderPolicy.parse(item.reminders)
        require(item.state in listOf("ACTIVE", "ARCHIVED", "USED", "DISCARDED")) {
            "Unknown status"
        }
        require(
            item.category.isNotBlank() &&
                item.profile.isNotBlank() &&
                item.category.length <= 200 &&
                item.profile.length <= 200
        ) {
            "Choose a category and profile"
        }
    }

    suspend fun save(item: Item) {
        validate(item)
        database.withTransaction {
            dao.label(Label("category", item.category))
            dao.label(Label("profile", item.profile))
            dao.save(item.copy(name = item.name.trim(), updated = System.currentTimeMillis()))
        }
        ReminderScheduler.reconcile(context)
    }

    suspend fun state(item: Item, state: String) {
        ReminderScheduler.cancelItem(context, item.id)
        save(
            item.copy(
                state = state,
                completed =
                    if (state in listOf("USED", "DISCARDED")) LocalDate.now().toString() else "",
            )
        )
        ReminderScheduler.dismiss(context, item.id)
    }

    suspend fun delete(item: Item) {
        ReminderScheduler.cancelItem(context, item.id)
        dao.delete(item.id)
        ReminderScheduler.dismiss(context, item.id)
        ReminderScheduler.reconcile(context)
        deleteUnreferencedImages()
    }

    suspend fun reset() {
        ReminderScheduler.cancel(context)
        database.withTransaction {
            dao.clearItems()
            dao.clearHistory()
            dao.clearLabels()
        }
        File(context.filesDir, "images").deleteRecursively()
        seed()
        ReminderScheduler.reconcile(context)
    }

    suspend fun deleteLabel(label: Label) {
        require(
            dao.all().none {
                if (label.kind == "category") it.category == label.name
                else it.profile == label.name
            }
        ) {
            "This label is assigned to an item"
        }
        dao.deleteLabel(label.kind, label.name)
    }

    suspend fun deleteUnreferencedImages() {
        val names = dao.all().map { it.image }.toSet()
        File(context.filesDir, "images")
            .listFiles()
            ?.filter { it.name !in names }
            ?.forEach { it.delete() }
    }

    suspend fun importImage(uri: Uri): String =
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            require(opts.outWidth > 0 && opts.outHeight > 0) { "Cannot decode image" }
            var sample = 1
            while (opts.outWidth / sample > 1024 || opts.outHeight / sample > 1024) sample *= 2
            val bitmap =
                context.contentResolver.openInputStream(uri).use {
                    BitmapFactory.decodeStream(
                        it,
                        null,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    )
                } ?: error("Cannot decode image")
            val orientation =
                context.contentResolver.openInputStream(uri).use { stream ->
                    if (stream == null) 1
                    else
                        androidx.exifinterface.media
                            .ExifInterface(stream)
                            .getAttributeInt(
                                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                                1,
                            )
                }
            val matrix =
                android.graphics.Matrix().apply {
                    when (orientation) {
                        2 -> setScale(-1f, 1f)
                        3 -> setRotate(180f)
                        4 -> setScale(1f, -1f)
                        5 -> {
                            setRotate(90f)
                            postScale(-1f, 1f)
                        }
                        6 -> setRotate(90f)
                        7 -> {
                            setRotate(270f)
                            postScale(-1f, 1f)
                        }
                        8 -> setRotate(270f)
                    }
                }
            val oriented =
                if (matrix.isIdentity) bitmap
                else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val name = "${UUID.randomUUID()}.jpg"
            val file = File(File(context.filesDir, "images").apply { mkdirs() }, name)
            file.outputStream().use { oriented.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            if (oriented !== bitmap) oriented.recycle()
            bitmap.recycle()
            name
        }
}
