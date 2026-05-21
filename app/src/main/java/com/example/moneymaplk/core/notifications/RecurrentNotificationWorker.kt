package com.example.moneymaplk.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moneymaplk.data.repository.FirebaseRecurringPaymentRepository
import com.example.moneymaplk.data.repository.FirebasePaymentFollowUpRepository
import com.example.moneymaplk.data.repository.FirebaseTransactionRepository
import com.example.moneymaplk.domain.calculation.FinanceCalculator
import com.example.moneymaplk.domain.calculation.RecurringScheduleCalculator
import com.example.moneymaplk.domain.model.IncomeSource
import com.example.moneymaplk.domain.model.RecurringPayment
import com.example.moneymaplk.domain.model.Transaction
import com.example.moneymaplk.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class RecurrentNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val recurringRepository = FirebaseRecurringPaymentRepository()
    private val paymentFollowUpRepository = FirebasePaymentFollowUpRepository()
    private val transactionRepository = FirebaseTransactionRepository()

    override suspend fun doWork(): Result {
        val userId = recurringRepository.currentUserId ?: return Result.success()
        val payments = recurringRepository.loadRecurringPayments(userId).getOrElse {
            return Result.retry()
        }

        payments
            .filter { payment -> payment.isActive }
            .filter { payment ->
                RecurringScheduleCalculator.isDueToday(payment.nextDueDate) ||
                    RecurringScheduleCalculator.isOverdue(payment.nextDueDate)
            }
            .forEach { payment ->
                if (payment.autoConfirm) {
                    confirmAutomatically(payment)
                } else if (RecurringScheduleCalculator.isOverdue(payment.nextDueDate)) {
                    RecurrentNotificationHelper.notifyOverdue(applicationContext, payment)
                }
            }

        val followUps = paymentFollowUpRepository.loadPaymentFollowUps(userId).getOrElse {
            return Result.retry()
        }
        followUps
            .filter { followUp ->
                FinanceCalculator.calculateEffectiveFollowUpStatus(followUp)
                    .name == "OVERDUE"
            }
            .forEach { followUp ->
                RecurrentNotificationHelper.notifyOverdueFollowUp(applicationContext, followUp)
            }

        return Result.success()
    }

    private suspend fun confirmAutomatically(payment: RecurringPayment) {
        val nextDueDate = RecurringScheduleCalculator.nextDateWithinRepeatEnd(
            currentDueDate = payment.nextDueDate,
            frequency = payment.frequency,
            repeatEndDate = payment.repeatEndDate
        )
        transactionRepository.saveTransaction(payment.toTransaction()).getOrElse { return }
        recurringRepository.markOccurrenceConfirmed(payment, nextDueDate)
    }

    private fun RecurringPayment.toTransaction(): Transaction {
        return Transaction(
            userId = userId,
            type = type,
            title = title,
            category = category,
            incomeSource = if (type == TransactionType.INCOME) incomeSource ?: IncomeSource.OTHER else null,
            originalAmount = originalAmount,
            originalCurrency = originalCurrency,
            exchangeRateToLkr = exchangeRateToLkr,
            convertedAmountLkr = convertedAmountLkr,
            transactionDate = nextDueDate,
            monthId = SimpleDateFormat("yyyy-MM", Locale.US).format(nextDueDate.toDate()),
            paymentMethod = paymentMethod,
            note = note,
            isCommitted = type == TransactionType.EXPENSE && isCommitted,
            isDiscretionary = type == TransactionType.EXPENSE && isDiscretionary,
            isRecurring = true,
            recurringPaymentId = paymentId
        )
    }
}
