package com.example.expensetracker.utils

import java.security.MessageDigest

data class ParsedExpense(
    val amount: Double,
    val merchant: String,
    val dateMillis: Long,
    val smsHash: String
)

object SmsParser {
    private val amountRegex = Regex("(?:₹|Rs\\.?|INR)\\s?([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val debitRegex = Regex("(debited|paid|spent|sent)", RegexOption.IGNORE_CASE)
    private val ignoreRegex = Regex("(otp|offer|loan|reward|credited|refund|received)", RegexOption.IGNORE_CASE)
    private val merchantRegex = Regex("(?:to|at|towards)\\s+([A-Za-z0-9 .&_-]{2,})", RegexOption.IGNORE_CASE)

    fun parse(message: String, timestamp: Long): ParsedExpense? {
        if (ignoreRegex.containsMatchIn(message) || !debitRegex.containsMatchIn(message)) return null
        val amount = amountRegex.find(message)?.groupValues?.getOrNull(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null
        val merchant = merchantRegex.find(message)?.groupValues?.getOrNull(1)?.trim()?.take(40).orEmpty()
            .ifBlank { "Unknown Merchant" }

        return ParsedExpense(amount, merchant, timestamp, sha256("$message|$timestamp"))
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
