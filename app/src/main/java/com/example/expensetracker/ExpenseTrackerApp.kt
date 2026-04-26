package com.example.expensetracker

import android.app.Application
import androidx.room.Room
import com.example.expensetracker.data.ExpenseDatabase
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.SettingsRepository

class ExpenseTrackerApp : Application() {
    lateinit var repository: ExpenseRepository
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, ExpenseDatabase::class.java, "expense-db").build()
        repository = ExpenseRepository(db.expenseDao())
        settingsRepository = SettingsRepository(this)
    }
}
