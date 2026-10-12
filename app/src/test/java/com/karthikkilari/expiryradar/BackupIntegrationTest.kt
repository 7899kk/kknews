package com.karthikkilari.expiryradar

import android.net.Uri
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.karthikkilari.expiryradar.backup.BackupManager
import com.karthikkilari.expiryradar.data.*
import java.io.File
import java.util.zip.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

class TestRadarApplication : RadarApplication() {
    override fun onCreate() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestRadarApplication::class)
class BackupIntegrationTest {
    private lateinit var app: TestRadarApplication
    private lateinit var backup: BackupManager
    private lateinit var zip: File

    @Before
    fun setup() = runBlocking {
        app = RuntimeEnvironment.getApplication() as TestRadarApplication
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        app.repository.dao.clearItems()
        app.repository.dao.clearLabels()
        app.repository.dao.clearHistory()
        app.preferences.set(Preferences(onboarded = true))
        backup = BackupManager(app)
        zip = File(app.cacheDir, "roundtrip.zip")
    }

    @Test
    fun fullBackupRoundTripAndDuplicates() = runBlocking {
        val image = File(File(app.filesDir, "images").apply { mkdirs() }, "test-image.jpg")
        image.writeBytes(byteArrayOf(1, 2, 3, 4))
        val item =
            Item(
                name = "Milk",
                expiry = "2027-08-15",
                image = image.name,
                profile = "Kitchen",
                reminders = "30,7,0",
                state = "USED",
                completed = "2027-08-14",
            )
        app.repository.dao.save(item)
        app.repository.dao.label(Label("profile", "Kitchen"))
        app.repository.dao.history(History("key", item.id, item.name, "Reminder"))
        app.preferences.set(Preferences(onboarded = true, theme = "Dark"))
        backup.export(Uri.fromFile(zip))
        app.repository.dao.clearItems()
        app.repository.dao.clearLabels()
        app.repository.dao.clearHistory()
        image.delete()
        assertEquals(1, backup.restore(Uri.fromFile(zip)))
        val restored = app.repository.dao.item(item.id)!!
        assertEquals(item.copy(image = restored.image), restored)
        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4),
            File(File(app.filesDir, "images"), restored.image).readBytes(),
        )
        assertEquals("Dark", app.preferences.flow.first().theme)
        assertEquals(1, app.repository.dao.allHistory().size)
        assertEquals(0, backup.restore(Uri.fromFile(zip)))
    }

    @Test
    fun tamperedBackupDoesNotMutateData() = runBlocking {
        val item = Item(name = "Keep me", expiry = "2027-08-15")
        app.repository.dao.save(item)
        backup.export(Uri.fromFile(zip))
        val modified = File(app.cacheDir, "tampered.zip")
        ZipFile(zip).use { original ->
            ZipOutputStream(modified.outputStream()).use { out ->
                original.entries().asSequence().forEach { entry ->
                    out.putNextEntry(ZipEntry(entry.name))
                    val bytes = original.getInputStream(entry).readBytes()
                    out.write(
                        if (entry.name == "data.json")
                            bytes.decodeToString().replace("Keep me", "Tampered").toByteArray()
                        else bytes
                    )
                    out.closeEntry()
                }
            }
        }
        assertTrue(runCatching { backup.restore(Uri.fromFile(modified)) }.isFailure)
        assertEquals(listOf(item), app.repository.dao.all())
    }

    @Test
    fun zipTraversalRejected() = runBlocking {
        val bad = File(app.cacheDir, "bad.zip")
        ZipOutputStream(bad.outputStream()).use {
            it.putNextEntry(ZipEntry("../../escape"))
            it.write(byteArrayOf(1))
            it.closeEntry()
        }
        assertTrue(runCatching { backup.restore(Uri.fromFile(bad)) }.isFailure)
        assertTrue(app.repository.dao.all().isEmpty())
    }

    @Test
    fun csvRoundTrip() = runBlocking {
        val item =
            Item(
                name = "Milk, cream",
                notes = "Use \"soon\"\nCold shelf",
                expiry = "2027-08-15",
                price = 12.5,
                quantity = 2.0,
            )
        app.repository.dao.save(item)
        val csv = File(app.cacheDir, "data.csv")
        backup.exportCsv(Uri.fromFile(csv))
        app.repository.dao.clearItems()
        assertEquals(1, backup.importCsv(Uri.fromFile(csv)))
        assertEquals(item, app.repository.dao.item(item.id))
        assertEquals(0, backup.importCsv(Uri.fromFile(csv)))
    }

    @Test
    fun invalidCsvIsAtomic() = runBlocking {
        val original = Item(name = "Original")
        app.repository.dao.save(original)
        val csv = File(app.cacheDir, "invalid.csv")
        csv.writeText("id,name,expiry\nnew,Milk,2027-02-29\n")
        assertTrue(runCatching { backup.importCsv(Uri.fromFile(csv)) }.isFailure)
        assertEquals(listOf(original), app.repository.dao.all())
    }

    @Test
    fun resetAllData() = runBlocking {
        app.repository.dao.save(Item(name = "Milk"))
        app.repository.dao.history(History("key", "id", "Milk", "Reminder"))
        app.repository.reset()
        assertTrue(app.repository.dao.all().isEmpty())
        assertTrue(app.repository.dao.allHistory().isEmpty())
        assertEquals(defaultCategories.size + 1, app.repository.dao.allLabels().size)
    }
}
