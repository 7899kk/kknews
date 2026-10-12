package com.karthikkilari.expiryradar

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.karthikkilari.expiryradar.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private lateinit var db: RadarDatabase

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    RadarDatabase::class.java,
                )
                .build()
    }

    @After
    fun close() {
        db.close()
    }

    @Test
    fun createEditDelete() = runBlocking {
        val dao = db.dao()
        val item = Item(name = "Milk", expiry = "2027-12-30")
        dao.save(item)
        assertEquals("Milk", dao.item(item.id)?.name)
        dao.save(item.copy(name = "Milk batch 2"))
        assertEquals(1, dao.all().size)
        assertEquals("Milk batch 2", dao.item(item.id)?.name)
        dao.delete(item.id)
        assertTrue(dao.all().isEmpty())
    }

    @Test
    fun notificationHistoryDeduplication() = runBlocking {
        val h = History("item:date", "item", "Milk", "Expires today")
        assertTrue(db.dao().history(h) != -1L)
        assertEquals(-1L, db.dao().history(h))
    }

    @Test
    fun labelsAreUnique() = runBlocking {
        db.dao().label(Label("category", "Kitchen"))
        db.dao().label(Label("category", "Kitchen"))
        assertEquals(1, db.dao().allLabels().size)
    }

    @Test
    fun largeDataset() = runBlocking {
        val items = (0 until 10000).map { Item(name = "Item $it", expiry = "2027-12-30") }
        db.dao().saveAll(items)
        assertEquals(10000, db.dao().all().size)
    }
}
