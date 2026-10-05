package com.example.rajyaai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var speech: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Microphone permission চাইবে
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        } else {
            startVoice()
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

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoice()
        }
    }

    private fun startVoice() {

        speech = SpeechRecognizer.createSpeechRecognizer(this)

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
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "bn-BD"
            )
        }

        speech.setRecognitionListener(
            object : RecognitionListener {

                override fun onResults(results: Bundle?) {

                    val command =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )?.firstOrNull()

                    if (!command.isNullOrBlank()) {
                        handleCommand(command)
                    }

                    startListening(intent)
                }

                override fun onError(error: Int) {
                    startListening(intent)
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

        startListening(intent)
    }

    private fun startListening(intent: Intent) {
        speech.startListening(intent)
    }

    private fun handleCommand(command: String) {

        // এখানে তোমার আগের open-app এবং call
        // command code থাকবে।
    }

    override fun onDestroy() {
        if (::speech.isInitialized) {
            speech.destroy()
        }
        super.onDestroy()
    }
}
