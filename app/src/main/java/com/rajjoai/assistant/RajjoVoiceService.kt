package com.rajjoai.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
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

    private lateinit var recognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var tts: TextToSpeech

    private val handler = Handler(Looper.getMainLooper())

    private var waitingForCommand = false
    private var speaking = false

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

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

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
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            )
        }

        recognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: android.os.Bundle?
                ) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(
                    rmsdB: Float
                ) {}

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {}

                override fun onEndOfSpeech() {}

                override fun onPartialResults(
                    partialResults: android.os.Bundle?
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: android.os.Bundle?
                ) {}

                override fun onError(
                    error: Int
                ) {
                    if (!speaking) {
                        listenLater(700)
                    }
                }

                override fun onResults(
                    results: android.os.Bundle?
                ) {

                    val resultsList =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        resultsList
                            ?.firstOrNull()
                            ?.lowercase(
                                Locale("bn", "BD")
                            )
                            ?.trim()
                            ?: ""

                    if (text.isEmpty()) {
                        listenLater(500)
                    } else {
                        handleVoice(text)
                    }
                }
            }
        )

        listenLater(1000)
    }

    // ================================
    // VOICE HANDLER
    // ================================

    private fun handleVoice(
        text: String
    ) {

        // রাজ্য বন্ধ
        if (
            text.contains("রাজ্য বন্ধ") ||
            text.contains("রাজ্জো বন্ধ") ||
            text.contains("রাজ্য অফ")
        ) {

            speak("জি স্যার, রাজ্য বন্ধ করছি।")

            handler.postDelayed({
                stopSelf()
            }, 1800)

            return
        }

        val wakeWord =
            text.contains("রাজ্য") ||
            text.contains("রাজ্জো") ||
            text.contains("রাজ্জ") ||
            text.contains("rajjo")

        // শুধু রাজ্য বললে
        if (
            !waitingForCommand &&
            wakeWord
        ) {

            waitingForCommand = true

            speak("জি স্যার, বলুন।")

            return
        }

        // এখনো wake word পাওয়া যায়নি
        if (!waitingForCommand) {
            listenLater(400)
            return
        }

        waitingForCommand = false

        executeCommand(text)
    }

    // ================================
    // COMMANDS
    // ================================

    private fun executeCommand(
        command: String
    ) {

        // সময়
        if (
            command.contains("সময় কত") ||
            command.contains("সময় কত") ||
            command.contains("কয়টা বাজে") ||
            command.contains("কয়টা বাজে")
        ) {

            val time =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale("bn", "BD")
                ).format(Date())

            speak(
                "জি স্যার, এখন সময় $time।"
            )

            return
        }

        // তারিখ
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

            speak(
                "জি স্যার, আজ $date।"
            )

            return
        }

        // টর্চ ON
        if (
            command.contains("টর্চ চালু") ||
            command.contains("ফ্ল্যাশলাইট চালু") ||
            command.contains("ফ্ল্যাশ অন")
        ) {

            flashlight(true)
            return
        }

        // টর্চ OFF
        if (
            command.contains("টর্চ বন্ধ") ||
            command.contains("ফ্ল্যাশলাইট বন্ধ") ||
            command.contains("ফ্ল্যাশ অফ")
        ) {

            flashlight(false)
            return
        }

        // সেটিংস
        if (
            command == "সেটিংস" ||
            command.contains("সেটিংস খোলো") ||
            command.contains("সেটিংস খুলে দাও")
        ) {

            openSettings(
                Settings.ACTION_SETTINGS,
                "সেটিংস"
            )

            return
        }

        // WiFi
        if (
            command.contains("ওয়াইফাই সেটিংস") ||
            command.contains("ওয়াইফাই সেটিংস")
        ) {

            openSettings(
                Settings.ACTION_WIFI_SETTINGS,
                "ওয়াইফাই সেটিংস"
            )

            return
        }

        // Bluetooth
        if (
            command.contains("ব্লুটুথ সেটিংস")
        ) {

            openSettings(
                Settings.ACTION_BLUETOOTH_SETTINGS,
                "ব্লুটুথ সেটিংস"
            )

            return
        }

        // Camera
        if (
            command == "ক্যামেরা" ||
            command.contains("ক্যামেরা খোলো") ||
            command.contains("ক্যামেরা খুলে দাও")
        ) {

            openCamera()
            return
        }

        // Phone
        if (
            command.contains("ফোন খোলো") ||
            command.contains("ফোন খুলে দাও") ||
            command.contains("ডায়ালার") ||
            command.contains("ডায়ালার")
        ) {

            openDialer()
            return
        }

        // Google
        if (
            command.startsWith("গুগলে") ||
            command.startsWith("google")
        ) {

            val query =
                command
                    .replace("গুগলে সার্চ কর", "")
                    .replace("গুগলে সার্চ", "")
                    .replace("গুগলে", "")
                    .replace("google search", "")
                    .replace("google", "")
                    .trim()

            if (query.isEmpty()) {

                speak(
                    "জি স্যার, কী সার্চ করব?"
                )

            } else {

                googleSearch(query)
            }

            return
        }

        // YouTube
        if (
            command.startsWith("ইউটিউবে") ||
            command.startsWith("youtube")
        ) {

            val query =
                command
                    .replace("ইউটিউবে সার্চ কর", "")
                    .replace("ইউটিউবে সার্চ", "")
                    .replace("ইউটিউবে", "")
                    .replace("youtube search", "")
                    .replace("youtube", "")
                    .trim()

            if (query.isEmpty()) {

                openApp(
                    "com.google.android.youtube",
                    "ইউটিউব"
                )

            } else {

                youtubeSearch(query)
            }

            return
        }

        // Facebook / WhatsApp / Messenger ইত্যাদি
        if (openInstalledApp(command)) {
            return
        }

        speak(
            "স্যার, এই কাজটি এখনো আমার মধ্যে যোগ করা হয়নি।"
        )
    }

    // ================================
    // APP OPEN
    // ================================

    private fun openInstalledApp(
        command: String
    ): Boolean {

        var name =
            command
                .lowercase(
                    Locale("bn", "BD")
                )
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
            "launch",
            "start"
        )

        for (word in removeWords) {
            name = name.replace(word, " ")
        }

        name =
            name
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        if (name.isEmpty()) {
            return false
        }

        val aliases = mapOf(
            "ফেসবুক" to "facebook",
            "এফবি" to "facebook",
            "fb" to "facebook",
            "ইউটিউব" to "youtube",
            "হোয়াটসঅ্যাপ" to "whatsapp",
            "হোয়াটসঅ্যাপ" to "whatsapp",
            "মেসেঞ্জার" to "messenger",
            "ইনস্টাগ্রাম" to "instagram",
            "টিকটক" to "tiktok",
            "ক্রোম" to "chrome",
            "জিমেইল" to "gmail",
            "প্লে স্টোর" to "play store",
            "প্লেস্টোর" to "play store"
        )

        val target =
            aliases[name] ?: name

        val launcherIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(
                    Intent.CATEGORY_LAUNCHER
                )
            }

        val apps =
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.MATCH_ALL
            )

        for (info in apps) {

            val label =
                info.loadLabel(
                    packageManager
                )
                    .toString()
                    .lowercase(
                        Locale("bn", "BD")
                    )
                    .trim()

            if (
                label == target ||
                label.contains(target) ||
                target.contains(label)
            ) {

                val launchIntent =
                    packageManager
                        .getLaunchIntentForPackage(
                            info.activityInfo.packageName
                        )

                if (launchIntent != null) {

                    val appName =
                        info.loadLabel(
                            packageManager
                        ).toString()

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

                    }, 800)

                    return true
                }
            }
        }

        return false
    }

    // ================================
    // OPEN APP
    // ================================

    private fun openApp(
        packageName: String,
        appName: String
    ) {

        val intent =
            packageManager
                .getLaunchIntentForPackage(
                    packageName
                )

        if (intent == null) {

            speak(
                "স্যার, $appName ফোনে পাওয়া যাচ্ছে না।"
            )

            return
        }

        speak(
            "জি স্যার, $appName খুলছি।"
        )

        handler.postDelayed({

            try {

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

            } catch (_: Exception) {

                speak(
                    "স্যার, $appName খোলা যাচ্ছে না।"
                )
            }

        }, 800)
    }

    // ================================
    // SETTINGS
    // ================================

    private fun openSettings(
        action: String,
        name: String
    ) {

        speak(
            "জি স্যার, $name খুলছি।"
        )

        handler.postDelayed({

            try {

                startActivity(
                    Intent(action).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }
                )

            } catch (_: Exception) {

                speak(
                    "স্যার, $name খোলা যাচ্ছে না।"
                )
            }

        }, 800)
    }

    // ================================
    // CAMERA
    // ================================

    private fun openCamera() {

        speak(
            "জি স্যার, ক্যামেরা খুলছি।"
        )

        handler.postDelayed({

            try {

                startActivity(
                    Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                    ).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }
                )

            } catch (_: Exception) {

                speak(
                    "স্যার, ক্যামেরা খোলা যাচ্ছে না।"
                )
            }

        }, 800)
    }

    // ================================
    // DIALER
    // ================================

    private fun openDialer() {

        speak(
            "জি স্যার, ফোন খুলছি।"
        )

        handler.postDelayed({

            try {

                startActivity(
                    Intent(
                        Intent.ACTION_DIAL
                    ).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }
                )

            } catch (_: Exception) {

                speak(
                    "স্যার, ফোন খোলা যাচ্ছে না।"
                )
            }

        }, 800)
    }

    // ================================
    // GOOGLE
    // ================================

    private fun googleSearch(
        query: String
    ) {

        speak(
            "জি স্যার, গুগলে সার্চ করছি।"
        )

        handler.postDelayed({

            try {

                val url =
                    "https://www.google.com/search?q=" +
                            Uri.encode(query)

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    ).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }
                )

            } catch (_: Exception) {

                speak(
                    "স্যার, গুগল সার্চ খোলা যাচ্ছে না।"
                )
            }

        }, 800)
    }

    // ================================
    // YOUTUBE
    // ================================

    private fun youtubeSearch(
        query: String
    ) {

        speak(
            "জি স্যার, ইউটিউবে সার্চ করছি।"
        )

        handler.postDelayed({

            try {

                val url =
                    "https://www.youtube.com/results?search_query=" +
                            Uri.encode(query)

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    ).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }
                )

            } catch (_: Exception) {

                speak(
                    "স্যার, ইউটিউব সার্চ খোলা যাচ্ছে না।"
                )
            }

        }, 800)
    }

    // ================================
    // FLASHLIGHT
    // ================================

    private fun flashlight(
        turnOn: Boolean
    ) {

        try {

            val cameraManager =
                getSystemService(
                    CAMERA_SERVICE
                ) as CameraManager

            val cameraId =
                cameraManager
                    .cameraIdList
                    .firstOrNull()

            if (cameraId == null) {

                speak(
                    "স্যার, ফোনে ফ্ল্যাশ পাওয়া যাচ্ছে না।"
                )

                return
            }

            cameraManager.setTorchMode(
                cameraId,
                turnOn
            )

            if (turnOn) {

                speak(
                    "জি স্যার, টর্চ চালু করেছি।"
                )

            } else {

                speak(
                    "জি স্যার, টর্চ বন্ধ করেছি।"
                )
            }

        } catch (_: Exception) {

            speak(
                "স্যার, টর্চ নিয়ন্ত্রণ করা যাচ্ছে না।"
            )
        }
    }

    // ================================
    // LISTENING
    // ================================

    private fun listen() {

        if (speaking) {
            return
        }

        try {

            recognizer.cancel()

            recognizer.startListening(
                speechIntent
            )

        } catch (_: Exception) {

            listenLater(1000)
        }
    }

    private fun listenLater(
        delay: Long
    ) {

        handler.postDelayed({

            if (!speaking) {
                listen()
            }

        }, delay)
    }

    // ================================
    // SPEAK
    // ================================

    private fun speak(
        text: String
    ) {

        speaking = true

        try {
            recognizer.cancel()
        } catch (_: Exception) {
        }

        if (!::tts.isInitialized) {

            speaking = false
            listenLater(300)
            return
        }

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "RAJJO_SPEECH"
        )

        handler.postDelayed({

            speaking = false

            if (waitingForCommand) {
                listenLater(300)
            } else {
                listenLater(500)
            }

        }, 1800)
    }

    // ================================
    // NOTIFICATION
    // ================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    "rajjo_ai",
                    "রাজ্য AI",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                "rajjo_ai"
            )
                .setContentTitle(
                    "রাজ্য AI চালু আছে"
                )
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
                .setContentTitle(
                    "রাজ্য AI চালু আছে"
                )
                .setContentText(
                    "বলুন: রাজ্য"
                )
                .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
                )
                .setOngoing(true)
                .build()
        }
    }

    // ================================
    // DESTROY
    // ================================

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        try {
            recognizer.destroy()
        } catch (_: Exception) {
        }

        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }
}
