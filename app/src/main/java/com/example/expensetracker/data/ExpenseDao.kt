package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :start AND :end ORDER BY dateMillis DESC")
    fun observeByDateRange(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE dateMillis BETWEEN :start AND :end")
    fun observeSumInRange(start: Long, end: Long): Flow<Double?>

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}
