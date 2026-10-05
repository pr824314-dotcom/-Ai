package com.example.rajyaai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class MainActivity : Activity() {

    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                10
            )
        } else {
            startVoice()
        }
    }

    private fun startVoice() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : android.speech.RecognitionListener {

                override fun onResults(results: Bundle?) {
                    val words = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val command = words?.firstOrNull()

                    if (!command.isNullOrBlank()) {
                        executeCommand(command)
                    }

                    startListening()
                }

                override fun onError(error: Int) {
                    startListening()
                }

                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(i: Int, b: Bundle?) {}
            }
        )

        startListening()
    }

    private fun startListening() {
        try {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "bn-BD"
                )
            }

            recognizer?.startListening(intent)
        } catch (_: Exception) {
        }
    }

    private fun executeCommand(command: String) {

        val text = command.lowercase(Locale.getDefault())

        // রাজ্য AI বন্ধ
        if (
            text.contains("রাজ্য বন্ধ") ||
            text.contains("বন্ধ হও") ||
            text.contains("বন্ধ হয়ে যাও")
        ) {
            finishAndRemoveTask()
            return
        }

        // Settings
        if (text.contains("সেটিং") || text.contains("settings")) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            return
        }

        // Wi-Fi settings
        if (text.contains("ওয়াইফাই") || text.contains("wifi")) {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            return
        }

        // Bluetooth settings
        if (text.contains("ব্লুটুথ") || text.contains("bluetooth")) {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            return
        }

        // নম্বর ডায়াল
        if (text.contains("কল") || text.contains("call")) {

            val number = Regex("\\d{5,15}")
                .find(text)
                ?.value

            if (number != null) {
                startActivity(
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:$number")
                    )
                )
            }

            return
        }

        // ফোনের অ্যাপের নাম মিলিয়ে খুলবে
        val pm = packageManager

        for (app in pm.getInstalledApplications(0)) {

            val name = pm.getApplicationLabel(app)
                .toString()
                .lowercase(Locale.getDefault())

            if (
                text.contains(name) &&
                pm.getLaunchIntentForPackage(
                    app.packageName
                ) != null
            ) {
                startActivity(
                    pm.getLaunchIntentForPackage(
                        app.packageName
                    )
                )
                return
            }
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }
}
