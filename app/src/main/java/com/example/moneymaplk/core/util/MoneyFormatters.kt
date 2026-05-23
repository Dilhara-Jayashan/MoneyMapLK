package com.example.moneymaplk.core.util

import java.util.Locale

fun formatLkr(amount: Double): String {
    return "LKR ${String.format(Locale.US, "%,.0f", safeNumber(amount))}"
}

fun formatMoney(amount: Double, currency: String): String {
    return "${currency.uppercase(Locale.US)} ${String.format(Locale.US, "%,.0f", safeNumber(amount))}"
}

fun formatWholeNumber(value: Double): String {
    return String.format(Locale.US, "%,.0f", safeNumber(value))
}

fun formatPercent(value: Double): String {
    return "${String.format(Locale.US, "%.1f", safeNumber(value)).removeSuffix(".0")}%"
}

fun formatWholePercent(value: Double): String {
    return "${String.format(Locale.US, "%.0f", safeNumber(value).coerceIn(0.0, 100.0))}%"
}

private fun safeNumber(value: Double): Double {
    return if (value.isFinite()) value else 0.0
}
