package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["smsHash"], unique = true)]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val notes: String = "",
    val dateMillis: Long,
    val source: String = "MANUAL",
    val smsHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
