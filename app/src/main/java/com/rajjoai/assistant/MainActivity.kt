package com.rajjoai.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val micPermission = 100

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
            "\nতোমার ভয়েস অ্যাসিস্ট্যান্ট\n\n" +
            "Wake word: রাজ্য\n" +
            "Response: জি স্যার, বলুন।\n"
        info.textSize = 20f

        val startButton = Button(this)
        startButton.text = "রাজ্য AI চালু করুন"

        val stopButton = Button(this)
        stopButton.text = "রাজ্য AI বন্ধ করুন"

        layout.addView(title)
        layout.addView(info)
        layout.addView(startButton)
        layout.addView(stopButton)

        setContentView(layout)

        startButton.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    micPermission
                )
            } else {
                startRajjo()
            }
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, RajjoVoiceService::class.java))
        }
    }

    private fun startRajjo() {
        val intent = Intent(this, RajjoVoiceService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= 26) {
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
        super.onRequestPermissionsResult(requestCode, permissions, results)

        if (requestCode == micPermission &&
            results.isNotEmpty() &&
            results[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startRajjo()
        }
    }
}
