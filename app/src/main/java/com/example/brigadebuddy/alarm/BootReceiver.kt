package com.example.brigadebuddy.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        Log.d("DailyAlarmReceiver", "Receiver fired: ${intent?.action}")

        if (
            intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent?.action == "com.example.TEST_BOOT"
        ) {
            Log.d("DailyAlarmReceiver", "Rescheduling alarm now")
            Toast.makeText(context, "BOOT RECEIVED", Toast.LENGTH_LONG).show()
            DailyAlarmScheduler.scheduleDaily8AM(context)
        }
    }
}