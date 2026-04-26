package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.data.ExpenseEntity
import com.example.expensetracker.utils.CategoryMapper
import com.example.expensetracker.utils.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val app = context.applicationContext as ExpenseTrackerApp

        CoroutineScope(Dispatchers.IO).launch {
            if (!app.settingsRepository.smsTrackingEnabled.first()) return@launch
            Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { sms ->
                val parsed = SmsParser.parse(sms.messageBody, sms.timestampMillis) ?: return@forEach
                app.repository.addExpense(
                    ExpenseEntity(
                        amount = parsed.amount,
                        merchant = parsed.merchant,
                        category = CategoryMapper.map(parsed.merchant),
                        notes = "Auto-imported from SMS",
                        dateMillis = parsed.dateMillis,
                        source = "SMS",
                        smsHash = parsed.smsHash
                    )
                )
            }
        }
    }
}
