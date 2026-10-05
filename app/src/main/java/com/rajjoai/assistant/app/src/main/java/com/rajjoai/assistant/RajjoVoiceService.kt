package com.rajjoai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class RajjoVoiceService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var commandMode = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = Notification.Builder(this, "rajjo_voice")
            .setContentTitle("রাজ্য AI")
            .setContentText("রাজ্য AI শুনছে...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(10, notification)

        tts = TextToSpeech(this) {
            tts?.language = Locale("bn", "BD")
        }

        startListening()
    }

    private fun startListening() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("দুঃখিত স্যার, এই ফোনে speech recognition পাওয়া যাচ্ছে না।")
            return
        }

        recognizer?.destroy()

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onResults(results: Bundle?) {

                val matches =
                    results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                val text = matches?.firstOrNull()?.trim()?.lowercase()
                    ?: ""

                if (text.isEmpty()) {
                    restartListening()
                    return
                }

                if (!commandMode) {

                    if (containsWakeWord(text)) {
                        commandMode = true
                        speak("জি স্যার, বলুন।")
                    } else {
                        restartListening()
                    }

                } else {

                    commandMode = false
                    handleCommand(text)
                }
            }

            override fun onError(error: Int) {
                restartListening()
            }

            override fun onEndOfSpeech() {
            }

            override fun onReadyForSpeech(params: Bundle?) {
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onPartialResults(partialResults: Bundle?) {
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "bn-BD"
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            "bn-BD"
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_MAX_RESULTS,
            3
        )

        recognizer?.startListening(intent)
    }

    private fun containsWakeWord(text: String): Boolean {

        return text.contains("রাজ্য") ||
                text.contains("রাজ্জো") ||
                text.contains("rajjo")
    }

    private fun handleCommand(text: String) {

        when {

            text.contains("ইউটিউব") ||
                    text.contains("youtube") -> {

                speak("জি স্যার, ইউটিউব খুলছি।")

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.youtube.com")
                )

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

                restartListening()
            }

            text.contains("ক্রোম") ||
                    text.contains("chrome") ||
                    text.contains("ব্রাউজার") -> {

                speak("জি স্যার, ব্রাউজার খুলছি।")

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.google.com")
                )

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

                restartListening()
            }

            text.contains("সেটিংস") ||
                    text.contains("settings") -> {

                speak("জি স্যার, সেটিংস খুলছি।")

                val intent = Intent(
                    android.provider.Settings.ACTION_SETTINGS
                )

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

                restartListening()
            }

            text.contains("সময়") ||
                    text.contains("কয়টা বাজে") ||
                    text.contains("কটা বাজে") -> {

                val time = java.text.SimpleDateFormat(
                    "hh:mm a",
                    Locale("bn", "BD")
                ).format(java.util.Date())

                speak("স্যার, এখন সময় $time।")

                restartListening()
            }

            text.contains("তারিখ") ||
                    text.contains("আজকে কী বার") -> {

                val date = java.text.SimpleDateFormat(
                    "EEEE, dd MMMM yyyy",
                    Locale("bn", "BD")
                ).format(java.util.Date())

                speak("স্যার, আজ $date।")

                restartListening()
            }

            text.contains("বন্ধ") ||
                    text.contains("থাম") -> {

                speak("জি স্যার, রাজ্য AI বন্ধ করছি।")

                stopSelf()
            }

            else -> {

                speak(
                    "দুঃখিত স্যার, এই কাজটি এখনো আমার command তালিকায় নেই।"
                )

                restartListening()
            }
        }
    }

    private fun speak(text: String) {

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "rajjo_response"
        )
    }

    private fun restartListening() {

        android.os.Handler(mainLooper).postDelayed({

            if (!commandMode) {
                startListening()
            }

        }, 1200)
    }

    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            "rajjo_voice",
            "রাজ্য AI Voice",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {

        recognizer?.destroy()
        recognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
