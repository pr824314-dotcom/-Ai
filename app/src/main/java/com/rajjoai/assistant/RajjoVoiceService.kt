package com.rajjoai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
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
import android.hardware.camera2.CameraManager
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

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechIntent = Intent(
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
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
            )
        }

        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: android.os.Bundle?
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

                override fun onError(
                    error: Int
                ) {
                    if (!isSpeaking) {
                        startListeningAfterDelay(700)
                    }
                }

                override fun onResults(
                    results: android.os.Bundle?
                ) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches?.firstOrNull()
                            ?.lowercase(
                                Locale("bn", "BD")
                            )
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

    // =========================================================
    // VOICE COMMAND
    // =========================================================

    private fun handleVoiceCommand(command: String) {

        val text = command
            .lowercase(Locale("bn", "BD"))
            .trim()

        // রাজ্য বন্ধ
        if (
            text.contains("রাজ্য বন্ধ") ||
            text.contains("রাজ্জো বন্ধ") ||
            text.contains("রাজ্জ বন্ধ") ||
            text.contains("রাজ্য অফ") ||
            text.contains("রাজ্য থাম")
        ) {

            speak("জি স্যার, রাজ্য বন্ধ করছি।")

            handler.postDelayed({
                stopSelf()
            }, 1800)

            return
        }

        // Wake word
        val hasWakeWord =
            text.contains("রাজ্য") ||
            text.contains("রাজ্জো") ||
            text.contains("রাজ্জ") ||
            text.contains("rajjo")

        // শুধু "রাজ্য" বললে
        if (!waitingForCommand && hasWakeWord) {

            waitingForCommand = true

            speak("জি স্যার, বলুন।")

            return
        }

        // Wake word ছাড়া কিছু বললে
        if (!waitingForCommand) {
            startListeningAfterDelay(400)
            return
        }

        waitingForCommand = false

        executeCommand(text)
    }

    // =========================================================
    // COMMAND PROCESSOR
    // =========================================================

    private fun executeCommand(command: String) {

        // ---------------- TIME ----------------

        if (
            command.contains("সময় কত") ||
            command.contains("সময় কত") ||
            command.contains("কয়টা বাজে") ||
            command.contains("কয়টা বাজে") ||
            command.contains("এখন কয়টা") ||
            command.contains("এখন কয়টা")
        ) {

            val time =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale("bn", "BD")
                ).format(Date())

            speak("জি স্যার, এখন সময় $time।")
            return
        }

        // ---------------- DATE ----------------

        if (
            command.contains("আজকের তারিখ") ||
            command.contains("আজ কত তারিখ") ||
            command.contains("তারিখ কত")
        ) {

            val date =
                SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale("bn", "BD")
                ).format(Date())

            speak("জি স্যার, আজ $date।")
            return
        }

        // ---------------- DAY ----------------

        if (
            command.contains("আজ কি বার") ||
            command.contains("আজ কী বার") ||
            command.contains("আজ কোন বার") ||
            command.contains("আজকের বার")
        ) {

            val day =
                SimpleDateFormat(
                    "EEEE",
                    Locale("bn", "BD")
                ).format(Date())

            speak("জি স্যার, আজ $day।")
            return
        }

        // ---------------- FLASHLIGHT ON ----------------

        if (
            command.contains("ফ্ল্যাশলাইট চালু") ||
            command.contains("ফ্লাশলাইট চালু") ||
            command.contains("টর্চ চালু") ||
            command.contains("ফ্ল্যাশ অন") ||
            command.contains("টর্চ অন")
        ) {

            flashlight(true)
            return
        }

        // ---------------- FLASHLIGHT OFF ----------------

        if (
            command.contains("ফ্ল্যাশলাইট বন্ধ") ||
            command.contains("ফ্লাশলাইট বন্ধ") ||
            command.contains("টর্চ বন্ধ") ||
            command.contains("ফ্ল্যাশ অফ") ||
            command.contains("টর্চ অফ")
        ) {

            flashlight(false)
            return
        }

        // ---------------- SETTINGS ----------------

        if (
            command == "সেটিংস" ||
            command.contains("সেটিংস খোলো") ||
            command.contains("সেটিংস খুলে দাও") ||
            command.contains("settings")
        ) {

            speak("জি স্যার, সেটিংস খুলছি।")

            handler.postDelayed({

                try {

                    val intent =
                        Intent(
                            Settings.ACTION_SETTINGS
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                    startActivity(intent)

                } catch (_: Exception) {

                    speak(
                        "স্যার, সেটিংস খোলা যাচ্ছে না।"
                    )
                }

            }, 800)

            return
        }

        // ---------------- WIFI ----------------

        if (
            command.contains("ওয়াইফাই সেটিংস") ||
            command.contains("ওয়াইফাই সেটিংস") ||
            command.contains("wifi settings")
        ) {

            speak(
                "জি স্যার, ওয়াইফাই সেটিংস খুলছি।"
            )

            handler.postDelayed({

                try {

                    val intent =
                        Intent(
                            Settings.ACTION_WIFI_SETTINGS
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                    startActivity(intent)

                } catch (_: Exception) {

                    speak(
                        "স্যার, ওয়াইফাই সেটিংস খোলা যাচ্ছে না।"
                    )
                }

            }, 800)

            return
        }

        // ---------------- BLUETOOTH ----------------

        if (
            command.contains("ব্লুটুথ সেটিংস") ||
            command.contains("bluetooth settings")
        ) {

            speak(
                "জি স্যার, ব্লুটুথ সেটিংস খুলছি।"
            )

            handler.postDelayed({

                try {

                    val intent =
                        Intent(
                            Settings.ACTION_BLUETOOTH_SETTINGS
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                    startActivity(intent)

                } catch (_: Exception) {

                    speak(
                        "স্যার, ব্লুটুথ সেটিংস খোলা যাচ্ছে না।"
                    )
                }

            }, 800)

            return
        }

        // ---------------- CAMERA ----------------

        if (
            command == "ক্যামেরা" ||
            command.contains("ক্যামেরা খোলো") ||
            command.contains("ক্যামেরা খুলে দাও") ||
            command.contains("camera")
        ) {

            speak("জি স্যার, ক্যামেরা খুলছি।")

            handler.postDelayed({

                try {

                    val intent =
                        Intent(
                            android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                    startActivity(intent)

                } catch (_: Exception) {

                    speak(
                        "স্যার, ক্যামেরা খোলা যাচ্ছে না।"
                    )
                }

            }, 800)

            return
        }

        // ---------------- PHONE / DIALER ----------------

        if (
            command.contains("ফোন খোলো") ||
            command.contains("ফোন খুলে দাও") ||
            command.contains("ডায়ালার") ||
            command.contains("ডায়ালার") ||
            command.contains("dialer")
        ) {

            speak("জি স্যার, ফোন খুলছি।")

            handler.postDelayed({

                try {

                    val intent =
                        Intent(
                            Intent.ACTION_DIAL
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                    startActivity(intent)

                } catch (_: Exception) {

                    speak(
                        "স্যার, ফোন খোলা যাচ্ছে না।"
                    )
                }

            }, 800)

            return
        }

        // ---------------- GOOGLE SEARCH ----------------

        if (
            command.startsWith("গুগলে") ||
            command.startsWith("google") ||
            command.contains("গুগলে সার্চ")
        ) {

            val query =
                command
                    .replace(
                        "গুগলে সার্চ কর",
                        ""
                    )
                    .replace(
                        "গুগলে সার্চ",
                        ""
                    )
                    .replace(
                        "গুগলে",
                        ""
                    )
                    .replace(
                        "google search",
                        ""
                    )
                    .replace(
                        "google",
                        ""
                    )
                    .trim()

            if (query.isNotEmpty()) {

                googleSearch(query)

            } else {

                speak(
                    "জি স্যার, কী সার্চ করব?"
                )
            }

            return
        }

        // ---------------- YOUTUBE SEARCH ----------------

        if (
            command.startsWith("ইউটিউবে") ||
            command.startsWith("youtube") ||
            command.contains("ইউটিউবে সার্চ")
        ) {

            val query =
                command
                    .replace(
                        "ইউটিউবে সার্চ কর",
                        ""
                    )
                    .replace(
                        "ইউটিউবে সার্চ",
                        ""
                    )
                    .replace(
                        "ইউটিউবে",
                        ""
                    )
                    .replace(
                        "youtube search",
                        ""
                    )
                    .replace(
                        "youtube",
                        ""
                    )
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

        // ---------------- INSTALLED APPS ----------------

        if (openInstalledApp(command)) {
            return
        }

        // ---------------- UNKNOWN ----------------

        speak(
            "স্যার, এই কাজটি এখনো আমার মধ্যে যোগ করা হয়নি।"
        )
    }

    // =========================================================
    // INSTALLED APP OPENER
    // =========================================================

    private fun openInstalledApp(
        command: String
    ): Boolean {

        val pm = packageManager

        val launcherIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

        val apps =
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.MATCH_ALL
            )

        var requested =
            command
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
            requested =
                requested.replace(
                    word,
                    " "
                )
        }

        requested =
            requested
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        if (requested.isEmpty()) {
            return false
        }

        val aliases =
            mapOf(

                "ফেসবুক" to listOf(
                    "facebook"
                ),

                "এফবি" to listOf(
                    "facebook"
                ),

                "fb" to listOf(
                    "facebook"
                ),

                "ইউটিউব" to listOf(
                    "youtube"
                ),

                "youtube" to listOf(
                    "youtube"
                ),

                "হোয়াটসঅ্যাপ" to listOf(
                    "whatsapp"
                ),

                "হোয়াটসঅ্যাপ" to listOf(
                    "whatsapp"
                ),

                "হোয়াটস অ্যাপ" to listOf(
                    "whatsapp"
                ),

                "whatsapp" to listOf(
                    "whatsapp"
                ),

                "মেসেঞ্জার" to listOf(
                    "messenger"
                ),

                "messenger" to listOf(
                    "messenger"
                ),

                "ইনস্টাগ্রাম" to listOf(
                    "instagram"
                ),

                "instagram" to listOf(
                    "instagram"
                ),

                "টিকটক" to listOf(
                    "tiktok"
                ),

                "tiktok" to listOf(
                    "tiktok"
                ),

                "ক্রোম" to listOf(
                    "chrome"
                ),

                "chrome" to listOf(
                    "chrome"
                ),

                "জিমেইল" to listOf(
                    "gmail"
                ),

                "gmail" to listOf(
                    "gmail"
                ),

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

        val possibleNames =
            mutableListOf<String>()

        possibleNames.add(requested)

        aliases[requested]?.let {
            possibleNames.addAll(it)
        }

        for (info in apps) {

            val label =
                info.loadLabel(pm)
                    .toString()
                    .lowercase(
                        Locale("bn", "BD")
                    )
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
                            info.loadLabel(pm)
                                .toString()

                        speak(
                            "জি স্যার, $appName খুলছি।"
                        )

                        handler.postDelayed({

                            try {

                                launchIntent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )

                                startActivity(
                                    launchIntent
                                )

                            } catch (_: Exception) {

                                speak(
                                    "স্যার, অ্যাপটি খোলা যাচ্ছে না।"
                                )
                            }

                       
