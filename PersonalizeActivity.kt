package com.example.secondspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class PersonalizeActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences("second_space_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val scroll = ScrollView(this)
        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val currentAllowed = prefs.getStringSet("allowed_packages", emptySet())?.toMutableSet() ?: mutableSetOf()
        val checkMap = mutableMapOf<String, CheckBox>()

        for (resolveInfo in apps) {
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()

            val cb = CheckBox(this).apply {
                text = "$label ($pkg)"
                isChecked = currentAllowed.contains(pkg)
            }
            checkMap[pkg] = cb
            listContainer.addView(cb)
        }

        val btnSave = Button(this).apply {
            text = "Zapisz wybór aplikacji"
            setOnClickListener {
                val selected = checkMap.filterValues { it.isChecked }.keys.toSet()
                prefs.edit().putStringSet("allowed_packages", selected).apply()
                finish()
            }
        }

        scroll.addView(listContainer)
        root.addView(btnSave)
        root.addView(scroll)
        setContentView(root)
    }
}
