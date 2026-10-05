package com.example.rajyaai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class MainActivity : Activity() {

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                101
            )
        } else {
            startVoice()
        }
    }

    private fun startVoice() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        val speechIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        speechIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        speechIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "bn-BD"
        )

        speechRecognizer?.setRecognitionListener(
            object : android.speech.RecognitionListener {

                override fun onResults(results: Bundle?) {
                    val words =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val command = words?.firstOrNull()

                    if (!command.isNullOrEmpty()) {
                        runCommand(command)
                    }

                    listen(speechIntent)
                }

                override fun onError(error: Int) {
                    listen(speechIntent)
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}
                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        listen(speechIntent)
    }

    private fun listen(intent: Intent) {
        try {
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
        }
    }

    private fun runCommand(command: String) {

        val text =
            command.lowercase(Locale.getDefault())

        // রাজ্য AI বন্ধ
        if (
            text.contains("রাজ্য বন্ধ") ||
            text.contains("বন্ধ হয়ে যাও") ||
            text.contains("বন্ধ হও")
        ) {
            finishAndRemoveTask()
            return
        }

        // ইনস্টল করা যেকোনো অ্যাপের নাম খোঁজা
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)

        for (app in apps) {

            val appName =
                pm.getApplicationLabel(app)
                    .toString()
                    .lowercase(Locale.getDefault())

            if (
                text.contains(appName) &&
                pm.getLaunchIntentForPackage(
                    app.packageName
                ) != null
            ) {

                val launch =
                    pm.getLaunchIntentForPackage(
                        app.packageName
                    )

                startActivity(launch)
                return
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startVoice()
        }
    }

    override fun onDestroy() {

        speechRecognizer?.destroy()
        speechRecognizer = null

        super.onDestroy()
    }
}
