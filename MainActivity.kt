package com.example.secondspace

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var lastClickTime = 0L
    private lateinit var screenReceiver: ScreenStateReceiver

    private val prefs by lazy {
        getSharedPreferences("second_space_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Tapeta z aktualnego systemu
        try {
            val wallpaper = WallpaperManager.getInstance(this).drawable
            findViewById<ImageView>(R.id.ivWallpaper).setImageDrawable(wallpaper)
        } catch (_: Exception) { }

        // Rejestracja odbiornika wyłączenia ekranu w czasie działania aktywności
        screenReceiver = ScreenStateReceiver()
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        })

        // Przycisk / ikona przełączająca tryb na 2 szybkie kliknięcia
        val btnToggle = findViewById<Button>(R.id.btnDoubleTapSwitch)
        btnToggle.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - lastClickTime < 450L) {
                val nextFullMode = !prefs.getBoolean("is_full_mode", false)
                prefs.edit().putBoolean("is_full_mode", nextFullMode).apply()
                val msg = if (nextFullMode) "Tryb normalny (wszystkie apki)" else "Tryb Second Space (wybrane apki)"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                refreshUi()
            }
            lastClickTime = now
        }

        // Szybki przycisk do personalizacji (wybór apek + skróty do uprawnień)
        findViewById<Button>(R.id.btnPersonalize).setOnClickListener {
            startActivity(Intent(this, PersonalizeActivity::class.java))
        }

        // Bezpośrednie przekierowania do uprawnień (żeby nie szukać w menu)
        findViewById<Button>(R.id.btnPermOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.btnPermBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.btnPermDetails).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
    }

    private fun refreshUi() {
        val isFullMode = prefs.getBoolean("is_full_mode", false)
        val allowedSet = prefs.getStringSet("allowed_packages", emptySet()) ?: emptySet()

        findViewById<TextView>(R.id.tvModeStatus).text =
            if (isFullMode) "Tryb: NORMALNY (2x kliknij przełącznik aby wrócić)"
            else "Tryb: SECOND SPACE (2x kliknij przełącznik = normalny)"

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val allApps = pm.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }

        val visibleApps = if (isFullMode) {
            allApps
        } else {
            allApps.filter { allowedSet.contains(it.activityInfo.packageName) }
        }

        findViewById<GridView>(R.id.gridApps).adapter = AppGridAdapter(this, visibleApps, pm)
    }
}
