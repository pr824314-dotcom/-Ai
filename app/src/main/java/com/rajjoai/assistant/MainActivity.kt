package com.rajjoai.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val permissionRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        val title = TextView(this)
        title.text = "রাজ্য AI"
        title.textSize = 32f

        val info = TextView(this)
        info.text =
            "\nবাংলা Voice Assistant\n\n" +
            "Wake word: রাজ্য\n" +
            "উত্তর: জি স্যার, বলুন।\n\n" +
            "স্ক্রিন চালু রেখে Rajjo AI চালু রাখতে Start চাপুন।"
        info.textSize = 19f

        val start = Button(this)
        start.text = "রাজ্য AI চালু করুন"

        val stop = Button(this)
        stop.text = "রাজ্য AI বন্ধ করুন"

        layout.addView(title)
        layout.addView(info)
        layout.addView(start)
        layout.addView(stop)

        setContentView(layout)

        start.setOnClickListener {
            requestPermissionsAndStart()
        }

        stop.setOnClickListener {
            stopService(Intent(this, RajjoVoiceService::class.java))
        }
    }

    private fun requestPermissionsAndStart() {

        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(
                missing.toTypedArray(),
                permissionRequest
            )
        } else {
            startRajjo()
        }
    }

    private fun startRajjo() {

        val intent = Intent(this, RajjoVoiceService::class.java)

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            results
        )

        if (requestCode == permissionRequest) {

            val micGranted =
                checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

            if (micGranted) {
                startRajjo()
            }
        }
    }
}
