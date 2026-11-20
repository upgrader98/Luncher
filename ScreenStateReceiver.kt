package com.example.secondspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("second_space_prefs", Context.MODE_PRIVATE)
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Każde wyłączenie ekranu resetuje tryb z powrotem na ograniczony ("Second Space")
                prefs.edit().putBoolean("is_full_mode", false).apply()
            }
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_USER_PRESENT -> {
                prefs.edit().putBoolean("is_full_mode", false).apply()
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
