package com.rajjoai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RajjoVoiceService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private var waitingForCommand = false
    private var speaking = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startRajjoForeground()

        tts = TextToSpeech(this) { status ->

            if (status == TextToSpeech.SUCCESS) {

                val result = tts?.setLanguage(
                    Locale("bn", "BD")
                )

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts?.language = Locale("bn")
                }

                startListeningAfterDelay()
            }
        }
    }

    private fun startRajjoForeground() {

        val notification = Notification.Builder(
            this,
            "rajjo_voice"
        )
            .setContentTitle("রাজ্য AI")
            .setContentText("রাজ্য AI প্রস্তুত আছে")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun startListeningAfterDelay() {

        handler.postDelayed({

            if (!speaking) {
                startListening()
            }

        }, 900)
    }

    private fun startListening() {

        if (speaking) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            speak(
                "স্যার, এই ফোনে বাংলা speech recognition পাওয়া যাচ্ছে না।"
            )

            return
        }

        recognizer?.destroy()

        recognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                }

                override fun onBeginningOfSpeech() {
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {
                }

                override fun onError(error: Int) {

                    if (!speaking) {
                        startListeningAfterDelay()
                    }
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    val list =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        list?.firstOrNull()
                            ?.trim()
                            ?.lowercase(Locale("bn", "BD"))
                            ?: ""

                    if (text.isEmpty()) {
                        startListeningAfterDelay()
                        return
                    }

                    processSpeech(text)
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )

        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

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
            5
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PARTIAL_RESULTS,
            false
        )

        recognizer?.startListening(intent)
    }

    private fun processSpeech(text: String) {

        if (!waitingForCommand) {

            if (containsWakeWord(text)) {

                waitingForCommand = true

                speak(
                    "জি স্যার, বলুন।"
                )

            } else {

                startListeningAfterDelay()
            }

            return
        }

        waitingForCommand = false

        handleCommand(text)
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

                handler.postDelayed({
                    openUrl("https://www.youtube.com")
                }, 1500)
            }

            text.contains("গুগল") ||
                    text.contains("google") -> {

                speak("জি স্যার, গুগল খুলছি।")

                handler.postDelayed({
                    openUrl("https://www.google.com")
                }, 1500)
            }

            text.contains("ক্রোম") ||
                    text.contains("ব্রাউজার") ||
                    text.contains("chrome") -> {

                speak("জি স্যার, ব্রাউজার খুলছি।")

                handler.postDelayed({
                    openUrl("https://www.google.com")
                }, 1500)
            }

            text.contains("সেটিংস") ||
                    text.contains("settings") -> {

                speak("জি স্যার, সেটিংস খুলছি।")

                handler.postDelayed({

                    val intent =
                        Intent(Settings.ACTION_SETTINGS)

                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )

                    startActivity(intent)

                }, 1500)
            }

            text.contains("সময়") ||
                    text.contains("কয়টা বাজে") ||
                    text.contains("কটা বাজে") -> {

                val time =
                    SimpleDateFormat(
                        "hh:mm a",
                        Locale("bn", "BD")
                    ).format(Date())

                speak(
                    "স্যার, এখন সময় $time।"
                )
            }

            text.contains("তারিখ") ||
                    text.contains("আজকের তারিখ") -> {

                val date =
                    SimpleDateFormat(
                        "dd MMMM yyyy",
                        Locale("bn", "BD")
                    ).format(Date())

                speak(
                    "স্যার, আজকের তারিখ $date।"
                )
            }

            text.contains("আজকে কী বার") ||
                    text.contains("আজ কি বার") -> {

                val day =
                    SimpleDateFormat(
                        "EEEE",
                        Locale("bn", "BD")
                    ).format(Date())

                speak(
                    "স্যার, আজ $day।"
                )
            }

            text.contains("বন্ধ কর") ||
                    text.contains("বন্ধ হও") ||
                    text.contains("থাম") -> {

                speak(
                    "জি স্যার, রাজ্য AI বন্ধ করছি।"
                )

                handler.postDelayed({
                    stopSelf()
                }, 1500)
            }

            else -> {

                speak(
                    "দুঃখিত স্যার, এই কাজটি এখনো আমার command তালিকায় যোগ করা হয়নি।"
                )
            }
        }
    }

    private fun openUrl(url: String) {

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
        )

        startActivity(intent)
    }

    private fun speak(text: String) {

        speaking = true

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "rajjo_${System.currentTimeMillis()}"
        )

        handler.postDelayed({

            speaking = false

            if (!waitingForCommand) {
                startListening()
            } else {
                startListening()
            }

        }, 1800)
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    "rajjo_voice",
                    "রাজ্য AI Voice",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        recognizer?.destroy()
        recognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
