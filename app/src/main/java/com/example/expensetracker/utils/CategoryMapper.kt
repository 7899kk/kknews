package com.example.expensetracker.utils

object CategoryMapper {
    fun map(merchant: String): String {
        val m = merchant.lowercase()
        return when {
            listOf("zomato", "swiggy").any { it in m } -> "Food"
            listOf("uber", "ola").any { it in m } -> "Travel"
            listOf("amazon", "flipkart").any { it in m } -> "Shopping"
            listOf("recharge", "electricity", "bill").any { it in m } -> "Bills"
            else -> "Other"
        }
    }
}
