package com.example.moneymaplk.domain.calculation

import com.example.moneymaplk.domain.model.PaymentFollowUp
import com.example.moneymaplk.domain.model.PaymentFollowUpStatus
import com.example.moneymaplk.domain.model.RecurringFrequency
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Date

object FinanceCalculator {
    fun roundMoney(value: Double): Double {
        return round(value, MONEY_SCALE)
    }

    fun roundRate(value: Double): Double {
        return round(value, RATE_SCALE)
    }

    fun convertToLkr(
        originalAmount: Double,
        originalCurrency: String,
        exchangeRateToLkr: Double,
        baseCurrency: String = "LKR"
    ): Double {
        val convertedAmount = if (originalCurrency.equals(baseCurrency, ignoreCase = true)) {
            originalAmount
        } else {
            originalAmount * exchangeRateToLkr
        }
        return roundMoney(convertedAmount)
    }

    fun calculateRemainingGoalAmount(
        targetAmountLkr: Double,
        savedAmountLkr: Double
    ): Double {
        return roundMoney((targetAmountLkr - savedAmountLkr).coerceAtLeast(0.0))
    }

    fun calculateGoalProgress(
        savedAmountLkr: Double,
        targetAmountLkr: Double
    ): Double {
        if (targetAmountLkr <= 0.0) return 0.0
        return roundRate(((savedAmountLkr / targetAmountLkr) * 100.0).coerceIn(0.0, 100.0))
    }

    fun calculateSafeToSpend(
        currentSavingsLkr: Double,
        thisMonthIncomeLkr: Double,
        thisMonthExpensesLkr: Double,
        plannedSavingsAllocationLkr: Double,
        safeToSpendBufferLkr: Double,
        upcomingRequiredRecurringPaymentsLkr: Double = 0.0
    ): SafeToSpendResult {
        val rawAmount = roundMoney(
            currentSavingsLkr +
                thisMonthIncomeLkr -
                thisMonthExpensesLkr -
                plannedSavingsAllocationLkr -
                safeToSpendBufferLkr -
                upcomingRequiredRecurringPaymentsLkr
        )
        return SafeToSpendResult(
            rawAmountLkr = rawAmount,
            displayAmountLkr = rawAmount.coerceAtLeast(0.0),
            isZeroOrNegative = rawAmount <= 0.0
        )
    }

    fun calculateIncomeLeftRate(
        totalIncomeLkr: Double,
        totalExpensesLkr: Double
    ): Double {
        return if (totalIncomeLkr > 0.0) {
            roundRate(((totalIncomeLkr - totalExpensesLkr) / totalIncomeLkr) * 100.0)
        } else {
            0.0
        }
    }

    fun calculateSharePercentage(
        amountLkr: Double,
        totalLkr: Double
    ): Double {
        if (
            !java.lang.Double.isFinite(amountLkr) ||
            !java.lang.Double.isFinite(totalLkr) ||
            amountLkr <= 0.0 ||
            totalLkr <= 0.0
        ) {
            return 0.0
        }
        return roundRate(((amountLkr / totalLkr) * 100.0).coerceIn(0.0, 100.0))
    }

    fun calculateShareFraction(
        amountLkr: Double,
        totalLkr: Double
    ): Float {
        return (calculateSharePercentage(amountLkr, totalLkr) / 100.0)
            .coerceIn(0.0, 1.0)
            .toFloat()
    }

    fun calculateAvailableToAllocate(
        currentSavingsLkr: Double,
        thisMonthIncomeLkr: Double,
        thisMonthExpensesLkr: Double
    ): Double {
        return roundMoney(currentSavingsLkr + thisMonthIncomeLkr - thisMonthExpensesLkr)
    }

    fun calculateMonthlyRecurringEstimate(
        convertedAmountLkr: Double,
        frequency: RecurringFrequency
    ): Double {
        val monthlyEstimate = when (frequency) {
            RecurringFrequency.WEEKLY -> convertedAmountLkr * 4.0
            RecurringFrequency.MONTHLY -> convertedAmountLkr
            RecurringFrequency.YEARLY -> convertedAmountLkr / 12.0
        }
        return roundMoney(monthlyEstimate.coerceAtLeast(0.0))
    }

    fun calculateEffectiveFollowUpStatus(followUp: PaymentFollowUp): PaymentFollowUpStatus {
        return calculateEffectiveFollowUpStatus(
            status = followUp.status,
            expectedDate = followUp.expectedDate.toDate()
        )
    }

    fun calculateEffectiveFollowUpStatus(
        status: PaymentFollowUpStatus,
        expectedDate: Date,
        today: Date = startOfToday()
    ): PaymentFollowUpStatus {
        return when (status) {
            PaymentFollowUpStatus.CONFIRMED -> PaymentFollowUpStatus.CONFIRMED
            PaymentFollowUpStatus.SKIPPED -> PaymentFollowUpStatus.SKIPPED
            PaymentFollowUpStatus.OVERDUE -> PaymentFollowUpStatus.OVERDUE
            PaymentFollowUpStatus.EXPECTED -> {
                if (expectedDate.before(today)) {
                    PaymentFollowUpStatus.OVERDUE
                } else {
                    PaymentFollowUpStatus.EXPECTED
                }
            }
        }
    }

    private fun round(value: Double, scale: Int): Double {
        if (!java.lang.Double.isFinite(value)) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(scale, RoundingMode.HALF_UP)
            .toDouble()
    }

    private fun startOfToday(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private const val MONEY_SCALE = 2
    private const val RATE_SCALE = 1
}

data class SafeToSpendResult(
    val rawAmountLkr: Double,
    val displayAmountLkr: Double,
    val isZeroOrNegative: Boolean
)
