package com.example.expensetracker.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {
    fun observeAllExpenses(): Flow<List<ExpenseEntity>> = dao.observeAllExpenses()

    fun observeMonthlyTotal(start: Long, end: Long): Flow<Double?> = dao.observeSumInRange(start, end)

    suspend fun addExpense(expense: ExpenseEntity): Long = dao.insert(expense)

    suspend fun clearAll() = dao.clearAll()
}
