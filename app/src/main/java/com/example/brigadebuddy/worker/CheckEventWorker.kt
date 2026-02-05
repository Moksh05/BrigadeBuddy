package com.example.brigadebuddy.worker

import android.Manifest
import android.annotation.SuppressLint
import com.example.brigadebuddy.model.RelationType
import com.example.brigadebuddy.model.Gender
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.brigadebuddy.db.AppDatabase
import com.example.brigadebuddy.db.EventEntity
import com.example.brigadebuddy.utils.DateHelper
import com.example.brigadebuddy.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId


class CheckEventsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CheckEventsWorker"
    }
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("DailyAlarmReceiver", "Worker started (triggered by AlarmManager)")
            val db = AppDatabase.getInstance(applicationContext)
            val eventDao = db.eventDao()


            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val tomorrow = today.plusDays(1)

            val events = eventDao.getAllActiveEvents()
            Log.d(TAG, "Found ${events.size} events in DB")
            // If we don't have permission, just skip showing notifications
            if (!hasPostNotificationPermission()) {
                Log.d(TAG, "No POST_NOTIFICATIONS permission, skipping notifications")
                return@withContext Result.success()
            }

            events.forEach { event ->
                Log.d(TAG, "Checking event: ${event.firstname} on ${event.day}-${event.month}-${event.year}")

                if (isEventTomorrow(event, tomorrow)) {
                    Log.d(TAG, "Event is for tomorrow, building notification...")
                    val years = calculateYearsCompleted(event, tomorrow)

                    // Skip invalid / future years
                    if (years < 0) return@forEach

                    val ordinal = DateHelper.getOrdinalSuffix(years)
                    val message = buildNotificationMessage(event, years, ordinal)

                    NotificationHelper.showEventNotification(applicationContext, event, message)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun isEventTomorrow(event: EventEntity, tomorrow: LocalDate): Boolean {
        return event.day == tomorrow.dayOfMonth && event.month == tomorrow.monthValue
    }

    private fun calculateYearsCompleted(event: EventEntity, tomorrow: LocalDate): Int {
        return try {
            val eventDate = LocalDate.of(event.year, event.month, event.day)
            Period.between(eventDate, tomorrow).years
        } catch (e: Exception) {
            -1
        }
    }

    private fun buildNotificationMessage(
        event: EventEntity,
        years: Int,
        ordinal: String
    ): String {

        val officer = "${event.rank} ${event.firstname}".trim()

        return when (event.relationType) {

            RelationType.SELF -> {
                "Tomorrow is $officer’s $ordinal birthday. Don’t forget to wish them!"
            }

            RelationType.SPOUSE -> {
                val relation =
                    if (event.gender == Gender.MALE) "husband"
                    else "wife"

                "Tomorrow is $officer’s $relation ${event.relatedName}’s $ordinal birthday."
            }

            RelationType.CHILD -> {
                val relation =
                    if (event.gender == Gender.MALE) "son"
                    else "daughter"

                "Tomorrow is $officer’s $relation ${event.relatedName}’s $ordinal birthday."
            }

            RelationType.ANNIVERSARY -> {
                "Tomorrow is $officer & ${event.relatedName}’s $ordinal anniversary."
            }

            RelationType.WORK -> {
                "Tomorrow is $officer’s $ordinal work anniversary."
            }

            else -> {
                "Tomorrow is $officer’s $ordinal ${event.title.lowercase()}."
            }
        }
    }


    private fun hasPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
