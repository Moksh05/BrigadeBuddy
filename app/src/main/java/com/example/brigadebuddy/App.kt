package com.example.brigadebuddy

import android.app.Application
import android.util.Log
import com.example.brigadebuddy.worker.WorkManagerScheduler


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("CheckEventsWorker", "worker scheduled")
        // Schedule WorkManager once
        //WorkManagerScheduler.scheduleDailyCheck(this)
    }
}