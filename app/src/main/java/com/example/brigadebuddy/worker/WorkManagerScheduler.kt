package com.example.brigadebuddy.worker

import android.content.Context
import android.util.Log

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {
    private const val UNIQUE_WORK_NAME = "daily_event_check"

    fun scheduleDailyCheck(context: Context) {

        val workRequest = PeriodicWorkRequestBuilder<CheckEventsWorker>(
            24, TimeUnit.HOURS
        ).build()
        Log.d("CheckEventsWorker", "scheduling worker for first time, 15 min")

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,   // prevents duplicates
            workRequest
        )
    }
}