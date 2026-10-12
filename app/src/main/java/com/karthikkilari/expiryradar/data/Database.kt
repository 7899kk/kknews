package com.karthikkilari.expiryradar.data

import androidx.room.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "items",
    indices = [Index("expiry"), Index("barcode"), Index("profile"), Index("category")],
)
data class Item(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Other",
    val profile: String = "My Items",
    val expiry: String = LocalDate.now().toString(),
    val purchase: String = "",
    val manufacturing: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Pieces",
    val location: String = "",
    val image: String = "",
    val barcode: String = "",
    val notes: String = "",
    val reminders: String = "7,3,1,0",
    val warning: Int = -1,
    val state: String = "ACTIVE",
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    val price: Double? = null,
    val completed: String = "",
)

@Serializable
@Entity(tableName = "labels", primaryKeys = ["kind", "name"])
data class Label(val kind: String, val name: String)

@Serializable
@Entity(tableName = "history")
data class History(
    @PrimaryKey val key: String,
    val itemId: String,
    val name: String,
    val message: String,
    val time: Long = System.currentTimeMillis(),
)

@Dao
interface RadarDao {
    @Query("SELECT * FROM items ORDER BY expiry") fun observe(): Flow<List<Item>>

    @Query("SELECT * FROM items") suspend fun all(): List<Item>

    @Query("SELECT * FROM items WHERE id=:id") suspend fun item(id: String): Item?

    @Upsert suspend fun save(item: Item)

    @Upsert suspend fun saveAll(items: List<Item>)

    @Query("DELETE FROM items WHERE id=:id") suspend fun delete(id: String)

    @Query("DELETE FROM items") suspend fun clearItems()

    @Query("SELECT * FROM labels ORDER BY name") fun labels(): Flow<List<Label>>

    @Query("SELECT * FROM labels") suspend fun allLabels(): List<Label>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun label(label: Label)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun labels(labels: List<Label>)

    @Query("DELETE FROM labels WHERE kind=:kind AND name=:name")
    suspend fun deleteLabel(kind: String, name: String)

    @Query("SELECT * FROM history ORDER BY time DESC LIMIT 200") fun history(): Flow<List<History>>

    @Query("SELECT * FROM history") suspend fun allHistory(): List<History>

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun history(history: History): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun histories(history: List<History>)

    @Query("DELETE FROM history WHERE `key`=:key") suspend fun deleteHistory(key: String)

    @Query("DELETE FROM history") suspend fun clearHistory()

    @Query("DELETE FROM labels") suspend fun clearLabels()
}

@Database(entities = [Item::class, Label::class, History::class], version = 1, exportSchema = true)
abstract class RadarDatabase : RoomDatabase() {
    abstract fun dao(): RadarDao
}

val defaultCategories =
    listOf(
        "Food & Beverages",
        "Groceries",
        "Medicines",
        "Cosmetics",
        "Personal Care",
        "Household Products",
        "Documents",
        "Electronics",
        "Warranties",
        "Pet Supplies",
        "Other",
    )
