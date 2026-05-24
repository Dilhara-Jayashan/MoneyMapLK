package com.example.moneymaplk.domain.calculation

import com.example.moneymaplk.domain.model.RecurringFrequency
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date

object RecurringScheduleCalculator {
    fun startOfToday(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    fun nextDateAfter(
        date: Date,
        frequency: RecurringFrequency
    ): Date {
        return Calendar.getInstance().apply {
            time = date
            when (frequency) {
                RecurringFrequency.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                RecurringFrequency.MONTHLY -> add(Calendar.MONTH, 1)
                RecurringFrequency.YEARLY -> add(Calendar.YEAR, 1)
            }
        }.time
    }

    fun nextTimestampAfter(
        timestamp: Timestamp,
        frequency: RecurringFrequency
    ): Timestamp {
        return Timestamp(nextDateAfter(timestamp.toDate(), frequency))
    }

    fun isDueToday(timestamp: Timestamp, today: Date = startOfToday()): Boolean {
        val dueDate = startOfDay(timestamp.toDate())
        return dueDate.time == today.time
    }

    fun isOverdue(timestamp: Timestamp, today: Date = startOfToday()): Boolean {
        return startOfDay(timestamp.toDate()).before(today)
    }

    fun canRepeatAtLeastOnce(
        startDate: Date,
        frequency: RecurringFrequency,
        repeatEndDate: Date
    ): Boolean {
        val firstRepeat = startOfDay(nextDateAfter(startDate, frequency))
        val end = startOfDay(repeatEndDate)
        return !end.before(firstRepeat)
    }

    fun nextDateWithinRepeatEnd(
        currentDueDate: Timestamp,
        frequency: RecurringFrequency,
        repeatEndDate: Timestamp?
    ): Timestamp? {
        val next = nextTimestampAfter(currentDueDate, frequency)
        return if (repeatEndDate != null && next.toDate().after(endOfDay(repeatEndDate.toDate()))) {
            null
        } else {
            next
        }
    }

    private fun startOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun endOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }
}
