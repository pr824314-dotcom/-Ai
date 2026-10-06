package com.rajjoai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
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

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var tts: TextToSpeech

    private val handler = Handler(Looper.getMainLooper())

    private var waitingForCommand = false
    private var isSpeaking = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(1001, createNotification())

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("bn", "BD")
                tts.setSpeechRate(0.95f)
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: android.os.Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (!isSpeaking) {
                        startListeningAfterDelay(700)
                    }
                }

                override fun onResults(results: android.os.Bundle?) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text = matches?.firstOrNull()
                        ?.lowercase(Locale("bn", "BD"))
                        ?.trim()
                        ?: ""

                    if (text.isNotEmpty()) {
                        handleVoiceCommand(text)
                    } else {
                        startListeningAfterDelay(500)
                    }
                }

                override fun onPartialResults(
                    partialResults: android.os.Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: android.os.Bundle?
                ) {
                }
            }
        )

        startListeningAfterDelay(1000)
    }

    // ---------------------------------------------------------
    // VOICE COMMAND
    // ---------------------------------------------------------

    private fun handleVoiceCommand(command: String) {

        val text = command
            .lowercase(Locale("bn", "BD"))
            .trim()

        // রাজ্য বন্ধ
        if (
            text.contains("রাজ্য বন্ধ") ||
            text.contains("রাজ্জো বন্ধ") ||
            text.contains("রাজ্জ বন্ধ") ||
            text.contains("rajjo বন্ধ") ||
            text.contains("রাজ্য থাম") ||
            text.contains("রাজ্য অফ")
        ) {
            speak("জি স্যার, রাজ্য বন্ধ করছি।")

            handler.postDelayed({
                stopSelf()
            }, 1300)

            return
        }

        // Wake word পাওয়া গেছে
        val hasWakeWord =
            text.contains("রাজ্য") ||
            text.contains("রাজ্জো") ||
            text.contains("রাজ্জ") ||
            text.contains("rajjo")

        if (!waitingForCommand && hasWakeWord) {

            waitingForCommand = true

            speak("জি স্যার, বলুন।")

            handler.postDelayed({
                startListening()
            }, 1300)

            return
        }

        // Wake word ছাড়া সরাসরি command এলে
        if (!waitingForCommand) {
            startListeningAfterDelay(400)
            return
        }

        waitingForCommand = false

        executeCommand(text)
    }

    // ---------------------------------------------------------
    // COMMAND PROCESSOR
    // ---------------------------------------------------------

    private fun executeCommand(command: String) {

        // সময়
        if (
            command.contains("সময় কত") ||
            command.contains("কয়টা বাজে") ||
            command.contains("সময় কত") ||
            command.contains("এখন কয়টা")
        ) {
            val time = SimpleDateFormat(
                "hh:mm a",
                Locale("bn", "BD")
            ).format(Date())

            speak("জি স্যার, এখন সময় $time।")
            return
        }

        // তারিখ
        if (
            command.contains("আজকের তারিখ") ||
            command.contains("আজ কত তারিখ") ||
            command.contains("তারিখ কত")
        ) {
            val date = SimpleDateFormat(
                "dd MMMM yyyy",
                Locale("bn", "BD")
            ).format(Date())

            speak("জি স্যার, আজ $date।")
            return
        }

        // দিন
        if (
            command.contains("আজ কি বার") ||
            command.contains("আজ কোন বার") ||
            command.contains("আজকের বার")
        ) {
            val day = SimpleDateFormat(
                "EEEE",
                Locale("bn", "BD")
            ).format(Date())

            speak("জি স্যার, আজ $day।")
            return
        }

        // Flashlight ON
        if (
            command.contains("ফ্ল্যাশলাইট চালু") ||
            command.contains("টর্চ চালু") ||
            command.contains("ফ্ল্যাশ অন") ||
            command.contains("টর্চ অন")
        ) {
            flashlight(true)
            return
        }

        // Flashlight OFF
        if (
            command.contains("ফ্ল্যাশলাইট বন্ধ") ||
            command.contains("টর্চ বন্ধ") ||
            command.contains("ফ্ল্যাশ অফ") ||
            command.contains("টর্চ অফ")
        ) {
            flashlight(false)
            return
        }

        // Settings
        if (
            command == "সেটিংস" ||
            command.contains("সেটিংস খোলো") ||
            command.contains("সেটিংস খুলে দাও") ||
            command.contains("settings")
        ) {
            speak("জি স্যার, সেটিংস খুলছি।")

            handler.postDelayed({
                try {
                    val intent = Intent(
                        Settings.ACTION_SETTINGS
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    startActivity(intent)
                } catch (_: Exception) {
                    speak("স্যার, সেটিংস খোলা যাচ্ছে না।")
                }
            }, 700)

            return
        }

        // Wi-Fi settings
        if (
            command.contains("ওয়াইফাই সেটিংস") ||
            command.contains("ওয়াইফাই সেটিংস") ||
            command.contains("wifi settings")
        ) {
            speak("জি স্যার, ওয়াইফাই সেটিংস খুলছি।")

            handler.postDelayed({
                try {
                    val intent = Intent(
                        Settings.ACTION_WIFI_SETTINGS
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    startActivity(intent)
                } catch (_: Exception) {
                    speak("স্যার, ওয়াইফাই সেটিংস খোলা যাচ্ছে না।")
                }
            }, 700)

            return
        }

        // Bluetooth settings
        if (
            command.contains("ব্লুটুথ সেটিংস") ||
            command.contains("bluetooth settings")
        ) {
            speak("জি স্যার, ব্লুটুথ সেটিংস খুলছি।")

            handler.postDelayed({
                try {
                    val intent = Intent(
                        Settings.ACTION_BLUETOOTH_SETTINGS
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    startActivity(intent)
                } catch (_: Exception) {
                    speak("স্যার, ব্লুটুথ সেটিংস খোলা যাচ্ছে না।")
                }
            }, 700)

            return
        }

        // Camera
        if (
            command == "ক্যামেরা" ||
            command.contains("ক্যামেরা খোলো") ||
            command.contains("ক্যামেরা খুলে দাও") ||
            command.contains("camera")
        ) {
            speak("জি স্যার, ক্যামেরা খুলছি।")

            handler.postDelayed({
                try {
                    val intent = Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    startActivity(intent)
                } catch (_: Exception) {
                    speak("স্যার, ক্যামেরা খোলা যাচ্ছে না।")
                }
            }, 700)

            return
        }

        // Phone / Dialer
        if (
            command.contains("ফোন খোলো") ||
            command.contains("ডায়ালার") ||
            command.contains("ডায়ালার") ||
            command.contains("dialer")
        ) {
            speak("জি স্যার, ফোন খুলছি।")

            handler.postDelayed({
                try {
                    val intent = Intent(
                        Intent.ACTION_DIAL
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    startActivity(intent)
                } catch (_: Exception) {
                    speak("স্যার, ফোন খোলা যাচ্ছে না।")
                }
            }, 700)

            return
        }

        // Google search
        if (
            command.startsWith("গুগলে") ||
            command.startsWith("google") ||
            command.contains("গুগলে সার্চ")
        ) {
            val query = command
                .replace("গুগলে সার্চ কর", "")
                .replace("গুগলে সার্চ", "")
                .replace("গুগলে", "")
                .replace("google search", "")
                .replace("google", "")
                .trim()

            if (query.isNotEmpty()) {
                googleSearch(query)
            } else {
                speak("জি স্যার, কী সার্চ করব?")
            }

            return
        }

        // YouTube search
        if (
            command.startsWith("ইউটিউবে") ||
            command.startsWith("youtube") ||
            command.contains("ইউটিউবে সার্চ")
        ) {
            val query = command
                .replace("ইউটিউবে সার্চ কর", "")
                .replace("ইউটিউবে সার্চ", "")
                .replace("ইউটিউবে", "")
                .replace("youtube search", "")
                .replace("youtube", "")
                .trim()

            if (query.isNotEmpty()) {
                youtubeSearch(query)
            } else {
                openAppByPackage(
                    "com.google.android.youtube",
                    "ইউটিউব"
                )
            }

            return
        }

        // Installed apps
        if (openInstalledApp(command)) {
            return
        }

        // সাধারণ উত্তর
        speak(
            "স্যার, এই কাজটি এখনো আমার মধ্যে যোগ করা হয়নি।"
        )
    }

    // ---------------------------------------------------------
    // INSTALLED APP OPENER
    // ---------------------------------------------------------

    private fun openInstalledApp(command: String): Boolean {

        val pm = packageManager

        val launcherIntent = Intent(
            Intent.ACTION_MAIN
        ).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = pm.queryIntentActivities(
            launcherIntent,
            PackageManager.MATCH_ALL
        )

        var requested = command
            .lowercase(Locale("bn", "BD"))
            .trim()

        val removeWords = listOf(
            "ওপেন করে দাও",
            "ওপেন কর",
            "ওপেন করো",
            "open করে দাও",
            "open কর",
            "open",
            "খুলে দাও",
            "খুলে দিও",
            "খোলো",
            "খুল",
            "চালু করে দাও",
            "চালু কর",
            "চালু করো",
            "চালু করে",
            "launch",
            "start"
        )

        for (word in removeWords) {
            requested = requested.replace(word, " ")
        }

        requested = requested
            .replace(Regex("\\s+"), " ")
            .trim()

        if (requested.isEmpty()) {
            return false
        }

        val aliases = mapOf(

            "ফেসবুক" to listOf("facebook"),
            "এফবি" to listOf("facebook"),
            "fb" to listOf("facebook"),

            "ইউটিউব" to listOf("youtube"),
            "youtube" to listOf("youtube"),

            "হোয়াটসঅ্যাপ" to listOf("whatsapp"),
            "হোয়াটসঅ্যাপ" to listOf("whatsapp"),
            "হোয়াটস অ্যাপ" to listOf("whatsapp"),
            "whatsapp" to listOf("whatsapp"),

            "মেসেঞ্জার" to listOf("messenger"),
            "messenger" to listOf("messenger"),

            "ইনস্টাগ্রাম" to listOf("instagram"),
            "instagram" to listOf("instagram"),

            "টিকটক" to listOf("tiktok"),
            "tiktok" to listOf("tiktok"),

            "ক্রোম" to listOf("chrome"),
            "chrome" to listOf("chrome"),

            "জিমেইল" to listOf("gmail"),
            "gmail" to listOf("gmail"),

            "প্লে স্টোর" to listOf(
                "play store",
                "google play"
            ),

            "প্লেস্টোর" to listOf(
                "play store",
                "google play"
            ),

            "play store" to listOf(
                "play store",
                "google play"
            )
        )

        val possibleNames = mutableListOf<String>()

        possibleNames.add(requested)

        aliases[requested]?.let {
            possibleNames.addAll(it)
        }

        for (info in apps) {

            val label = info.loadLabel(pm)
                .toString()
                .lowercase(Locale("bn", "BD"))
                .trim()

            for (name in possibleNames) {

                if (
                    label == name ||
                    label.contains(name) ||
                    name.contains(label)
                ) {

                    val launchIntent =
                        pm.getLaunchIntentForPackage(
                            info.activityInfo.packageName
                        )

                    if (launchIntent != null) {

                        val appName =
                            info.loadLabel(pm).toString()

                        speak(
                            "জি স্যার, $appName খুলছি।"
                        )

                        handler.postDelayed({

                            try {

                                launchIntent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )

                                startActivity(launchIntent)

                            } catch (_: Exception) {

                                speak(
                                    "স্যার, অ্যাপটি খোলা যাচ্ছে না।"
                                )
                            }

                        }, 800)

                        return true
                    }
                }
            }
        }

        return false
    }

    // ---------------------------------------------------------
    // OPEN APP BY PACKAGE
    // ---------------------------------------------------------

    private fun openAppByPackage(
        packageName: String,
        appName: String
    ) {

        val intent =
            packageManager.getLaunchIntentForPackage(
                packageName
            )

        if (intent == null) {
            speak("স্যার, $appName ফোনে পাওয়া যাচ্ছে না।")
            return
        }

        speak("জি স্যার, $appName খুলছি।")

        handler.postDelayed({

            try {
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

            } catch (_: Exception) {
                speak("স্যার, $appName খোলা যাচ্ছে না।")
            }

        }, 800)
    }

    // ---------------------------------------------------------
    // GOOGLE SEARCH
    // ---------------------------------------------------------

    private fun googleSearch(query: String) {

        speak("জি স্যার, গুগলে সার্চ করছি।")

        handler.postDelayed({

            try {

                val url =
                    "https://www.google.com/search?q=" +
                            Uri.encode(query)

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

                startActivity(intent)

            } catch (_: Exception) {

                speak(
                    "স্যার, গুগল সার্চ খোলা যাচ্ছে না।"
                )
            }

        }, 700)
    }

    // ---------------------------------------------------------
    // YOUTUBE SEARCH
    // ---------------------------------------------------------

    private fun youtubeSearch(query: String) {

        speak("জি স্যার, ইউটিউবে সার্চ করছি।")

        handler.postDelayed({

            try {

                val url =
                    "https://www.youtube.com/results?search_query=" +
                            Uri.encode(query)

                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                ).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

                startActivity(intent)

            } catch (_: Exception) {

                speak(
                    "স্যার, ইউটিউব সার্চ খোলা যাচ্ছে না।"
                )
            }

        }, 700)
    }

    // ---------------------------------------------------------
    // FLASHLIGHT
    // ---------------------------------------------------------

    private fun flashlight(turnOn: Boolean) {

        try {

            val cameraManager =
                getSystemService(
                    CAMERA_SERVICE
                ) as CameraManager

            val cameraId =
                cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                speak("স্যার, ফোনে ফ্ল্যাশ পাওয়া যাচ্ছে না।")
                return
            }

            cameraManager.setTorchMode(
                cameraId,
                turnOn
            )

            if (turnOn) {
                speak("জি স্যার, টর্চ চালু করেছি।")
            } else {
                speak("জি স্যার, টর্চ বন্ধ করেছি।")
            }

        } catch (_: SecurityException) {

            speak(
                "স্যার, টর্চ চালু করতে ক্যামেরার অনুমতি দরকার।"
            )

        } catch (_: Exception) {

            speak(
                "স্যার, টর্চ নিয়ন্ত্রণ করা যাচ্ছে না।"
            )
        }
    }

    // ---------------------------------------------------------
    // SPEECH
    // ---------------------------------------------------------

    private fun startListening() {

        if (isSpeaking) return

        try {
            speechRecognizer.cancel()
            speechRecognizer.startListening(speechIntent)
        } catch (_: Exception) {
            startListeningAfterDelay(1000)
        }
    }

    private fun startListeningAfterDelay(
        delay: Long
    ) {

        if (isSpeaking) return

        handler.postDelayed({

            if (!isSpeaking) {
                startListening()
            }

        }, delay)
    }

    private fun speak(text: String) {

        isSpeaking = true

        try {
            speechRecognizer.cancel()
        } catch (_: Exception) {
        }

        if (::tts.isInitialized) {

            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "RAJJO_SPEECH"
            )

            handler.postDelayed({

                isSpeaking = false

                if (!waitingForCommand) {
                    startListeningAfterDelay(500)
                }

            }, 1700)

        } else {

            isSpeaking = false
            startListeningAfterDelay(500)
        }
    }

    // ---------------------------------------------------------
    // NOTIFICATION
    // ---------------------------------------------------------

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "rajjo_ai_channel",
                "রাজ্য AI",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "রাজ্য AI Voice Assistant"
            }

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(
                this,
                "rajjo_ai_channel"
            )
                .setContentTitle("রাজ্য AI চালু আছে")
                .setContentText(
                    "বলুন: রাজ্য"
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()

        } else {

            Notification.Builder(this)
                .setContentTitle("রাজ্য AI চালু আছে")
                .setContentText("বলুন: রাজ্য")
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()
        }
    }

    // ---------------------------------------------------------
    // SERVICE STOP
    // ---------------------------------------------------------

    override fun onDestroy() {

        try {
            speechRecognizer.destroy()
        } catch (_: Exception) {
        }

        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
