package com.example.brigadebuddy.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.brigadebuddy.worker.CheckEventsWorker

class DailyAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {

        Log.d(TAG, "Alarm fired! Triggered by AlarmManager")

        val workRequest =
            OneTimeWorkRequestBuilder<CheckEventsWorker>().build()

        WorkManager.getInstance(context).enqueue(workRequest)

        Log.d(TAG, "CheckEventsWorker enqueued from AlarmManager")

        // IMPORTANT: reschedule next day
        DailyAlarmScheduler.scheduleDaily8AM(context)
    }
}
