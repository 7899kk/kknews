package com.example.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseEntity
import com.example.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModel(repository: ExpenseRepository) : ViewModel() {
    val expenses = repository.observeAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthlyTotal = expenses.map { list ->
        val now = LocalDate.now()
        list.filter { it.dateMillis.toLocalDate().month == now.month && it.dateMillis.toLocalDate().year == now.year }
            .sumOf(ExpenseEntity::amount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val todayTotal = expenses.map { list ->
        val today = LocalDate.now()
        list.filter { it.dateMillis.toLocalDate() == today }.sumOf(ExpenseEntity::amount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}

class DashboardViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(repository) as T
    }
}
