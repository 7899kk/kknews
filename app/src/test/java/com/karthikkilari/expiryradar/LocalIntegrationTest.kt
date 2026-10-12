package com.karthikkilari.expiryradar

import android.app.Application
import androidx.room.Room
import com.karthikkilari.expiryradar.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LocalIntegrationTest {
    private lateinit var db: RadarDatabase
    private lateinit var repo: Repository

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        db =
            Room.inMemoryDatabaseBuilder(context, RadarDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repo = Repository(context, db)
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun itemCreateEditDelete() = runBlocking {
        val item = Item(name = "Milk", expiry = "2027-08-15")
        repo.validate(item)
        db.dao().save(item)
        assertEquals(item, db.dao().item(item.id))
        db.dao().save(item.copy(name = "Edited"))
        assertEquals(1, db.dao().all().size)
        assertEquals("Edited", db.dao().item(item.id)?.name)
        db.dao().delete(item.id)
        assertTrue(db.dao().all().isEmpty())
    }

    @Test
    fun seedAndUniqueLabels() = runBlocking {
        repo.seed()
        repo.seed()
        assertEquals(defaultCategories.size + 1, db.dao().allLabels().size)
    }

    @Test
    fun refuseDeletingInUseCategory() = runBlocking {
        db.dao().save(Item(name = "Milk", category = "Groceries"))
        assertTrue(runCatching { repo.deleteLabel(Label("category", "Groceries")) }.isFailure)
    }

    @Test
    fun validationErrors() {
        listOf(
                Item(name = ""),
                Item(name = "Milk", expiry = "2027-02-29"),
                Item(name = "Milk", quantity = 0.0),
                Item(name = "Milk", quantity = Double.NaN),
                Item(name = "Milk", price = -1.0),
                Item(name = "Milk", reminders = "-1"),
                Item(name = "Milk", state = "BAD"),
            )
            .forEach { assertTrue(runCatching { repo.validate(it) }.isFailure) }
    }

    @Test
    fun multipleBatches() = runBlocking {
        val a = Item(name = "Milk", expiry = "2027-08-15")
        val b = Item(name = "Milk", expiry = "2027-08-16")
        db.dao().saveAll(listOf(a, b))
        assertEquals(2, db.dao().all().size)
    }

    @Test
    fun historyDedup() = runBlocking {
        val h = History("key", "item", "Milk", "Reminder")
        assertTrue(db.dao().history(h) >= 0)
        assertEquals(-1L, db.dao().history(h))
    }

    @Test
    fun preferencePersistence() = runBlocking {
        val store = PreferenceStore(RuntimeEnvironment.getApplication())
        val p =
            Preferences(
                onboarded = true,
                theme = "Dark",
                warning = 14,
                reminders = "30,14,0",
                dateFormat = "yyyy-MM-dd",
                mondayFirst = false,
            )
        store.set(p)
        assertEquals(p, store.flow.first())
    }

    @Test
    fun largeDataset() = runBlocking {
        val items = (0 until 10000).map { Item(name = "Item $it", expiry = "2027-12-30") }
        db.dao().saveAll(items)
        assertEquals(10000, db.dao().all().size)
        assertEquals(items[9999], db.dao().item(items[9999].id))
    }

    @Test
    fun deletedCategoryStaysDeletedOnReseed() = runBlocking {
        repo.seed()
        db.dao().deleteLabel("category", "Cosmetics")
        repo.seed()
        assertTrue(db.dao().allLabels().none { it.name == "Cosmetics" })
    }

    @Test
    fun recordsSurviveDatabaseReopen() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase("reopen-test.db")
        val item = Item(name = "Persistent milk", expiry = "2027-08-15")
        val first =
            Room.databaseBuilder(context, RadarDatabase::class.java, "reopen-test.db").build()
        first.dao().save(item)
        first.close()
        val reopened =
            Room.databaseBuilder(context, RadarDatabase::class.java, "reopen-test.db").build()
        assertEquals(item, reopened.dao().item(item.id))
        reopened.close()
        context.deleteDatabase("reopen-test.db")
        Unit
    }

    @Test
    fun missingExpiryRejected() {
        assertTrue(runCatching { repo.validate(Item(name = "Milk", expiry = "")) }.isFailure)
    }

    @Test
    fun importedDatesMustFitPickerRange() {
        assertTrue(
            runCatching { repo.validate(Item(name = "Milk", expiry = "3000-01-01")) }.isFailure
        )
    }
}
