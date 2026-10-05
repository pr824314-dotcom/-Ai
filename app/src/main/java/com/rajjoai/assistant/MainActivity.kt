package com.example.rajyaai

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var speech: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speech = SpeechRecognizer.createSpeechRecognizer(this)

        startListening()
    }

    private fun startListening() {

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
        }

        speech.setRecognitionListener(object :
            android.speech.RecognitionListener {

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                if (!text.isNullOrBlank()) {
                    handleCommand(text)
                }

                startListening()
            }

            override fun onError(error: Int) {
                startListening()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speech.startListening(intent)
    }

    private fun handleCommand(command: String) {

        val text = command.lowercase(Locale.getDefault())

        // রাজ্য AI বন্ধ
        if (
            text.contains("বন্ধ হও") ||
            text.contains("বন্ধ হয়ে যাও")
        ) {
            finish()
            return
        }

        // কল করার কমান্ড
        if (text.contains("কল") || text.contains("call")) {

            val number = Regex("""\d{5,15}""")
                .find(text)
                ?.value

            if (number != null) {
                val intent = Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:$number")
                )
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "নম্বরটি বলুন",
                    Toast.LENGTH_SHORT
                ).show()
            }

            return
        }

        // যেকোনো ইনস্টল করা অ্যাপ খোঁজা
        openInstalledApp(text)
    }

    private fun openInstalledApp(command: String) {

        val pm = packageManager
        val apps = pm.getInstalledApplications(0)

        for (app in apps) {

            val appName =
                pm.getApplicationLabel(app)
                    .toString()
                    .lowercase(Locale.getDefault())

            if (
                command.contains(appName) &&
                pm.getLaunchIntentForPackage(app.packageName) != null
            ) {

                val launchIntent =
                    pm.getLaunchIntentForPackage(app.packageName)

                startActivity(launchIntent)
                return
            }
        }

        Toast.makeText(
            this,
            "অ্যাপটি পাওয়া যায়নি",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        speech.destroy()
        super.onDestroy()
    }
}
