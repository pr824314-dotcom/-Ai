package com.rajjoai.assistant

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class RajjoVoiceService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private var waitingForCommand = false
    private var speaking = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = Notification.Builder(this, "rajjo_voice")
            .setContentTitle("রাজ্য AI")
            .setContentText("রাজ্য AI শুনছে")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("bn", "BD"))

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts?.language = Locale("bn")
                }

                startListeningAfterDelay(700)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "rajjo_voice",
                "রাজ্য AI Voice",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun startListeningAfterDelay(delay: Long = 500) {
        handler.postDelayed({
            if (!speaking) {
                startListening()
            }
        }, delay)
    }

    private fun startListening() {

        if (speaking) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("স্যার, এই ফোনে ভয়েস রিকগনিশন পাওয়া যাচ্ছে না।")
            return
        }

        recognizer?.destroy()

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

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

                override fun onError(error: Int) {
                    if (!speaking) {
                        startListeningAfterDelay(400)
                    }
                }

                override fun onResults(results: Bundle?) {

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
            }
        )

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        )

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

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            startListeningAfterDelay(1000)
        }
    }

    private fun processSpeech(text: String) {

        if (!waitingForCommand) {

            if (containsWakeWord(text)) {

                waitingForCommand = true

                speak("জি স্যার, বলুন।")

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
                text.contains("রাজ্জ") ||
                text.contains("rajjo")
    }

    private fun handleCommand(text: String) {

        val command = normalize(text)

        // =========================
        // রাজ্য বন্ধ
        // =========================

        if (
            command.contains("বন্ধ হও") ||
            command.contains("বন্ধ হয়ে যাও") ||
            command.contains("শোনা বন্ধ কর") ||
            command.contains("থেমে যাও")
        ) {

            speakAndStop(
                "জি স্যার, রাজ্য AI এখন বন্ধ হচ্ছে।"
            )

            return
        }

        // =========================
        // FLASH ON
        // =========================

        if (
            command.contains("ফ্ল্যাশ চালু") ||
            command.contains("ফ্লাশ চালু") ||
            command.contains("টর্চ চালু") ||
            command.contains("টর্চ অন") ||
            command.contains("flash on")
        ) {

            setFlash(true)

            return
        }

        // =========================
        // FLASH OFF
        // =========================

        if (
            command.contains("ফ্ল্যাশ বন্ধ") ||
            command.contains("ফ্লাশ বন্ধ") ||
            command.contains("টর্চ বন্ধ") ||
            command.contains("টর্চ অফ") ||
            command.contains("flash off")
        ) {

            setFlash(false)

            return
        }

        // =========================
        // SETTINGS
        // =========================

        if (
            command.contains("সেটিংস") ||
            command.contains("settings")
        ) {

            speak("জি স্যার, সেটিংস খুলছি।")

            openDelayed {
                Intent(Settings.ACTION_SETTINGS)
            }

            return
        }

        // =========================
        // WIFI SETTINGS
        // =========================

        if (
            command.contains("ওয়াইফাই") ||
            command.contains("wifi")
        ) {

            speak("জি স্যার, ওয়াইফাই সেটিংস খুলছি।")

            openDelayed {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }

            return
        }

        // =========================
        // BLUETOOTH SETTINGS
        // =========================

        if (
            command.contains("ব্লুটুথ") ||
            command.contains("bluetooth")
        ) {

            speak("জি স্যার, ব্লুটুথ সেটিংস খুলছি।")

            openDelayed {
                Intent(
                    Settings.ACTION_BLUETOOTH_SETTINGS
                )
            }

            return
        }

        // =========================
        // CAMERA
        // =========================

        if (
            command.contains("ক্যামেরা") ||
            command.contains("camera")
        ) {

            speak("জি স্যার, ক্যামেরা খুলছি।")

            openDelayed {
                Intent(
                    "android.media.action.IMAGE_CAPTURE"
                )
            }

            return
        }

        // =========================
        // PHONE / DIALER
        // =========================

        if (
            command.contains("ফোন খোলো") ||
            command.contains("ডায়ালার") ||
            command.contains("কল খোলো") ||
            command.contains("dialer")
        ) {

            speak("জি স্যার, ফোন খুলছি।")

            openDelayed {
                Intent(Intent.ACTION_DIAL)
            }

            return
        }

        // =========================
        // TIME
        // =========================

        if (
            command.contains("সময়") ||
            command.contains("কয়টা বাজে") ||
            command.contains("কটা বাজে") ||
            command.contains("কত বাজে")
        ) {

            val time =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale("bn", "BD")
                ).format(Date())

            speak("স্যার, এখন সময় $time।")

            return
        }

        // =========================
        // DATE
        // =========================

        if (
            command.contains("তারিখ") ||
            command.contains("আজকের তারিখ")
        ) {

            val date =
                SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale("bn", "BD")
                ).format(Date())

            speak("স্যার, আজকের তারিখ $date।")

            return
        }

        // =========================
        // DAY
        // =========================

        if (
            command.contains("আজ কী বার") ||
            command.contains("আজ কি বার") ||
            command.contains("আজকে কী বার")
        ) {

            val day =
                SimpleDateFormat(
                    "EEEE",
                    Locale("bn", "BD")
                ).format(Date())

            speak("স্যার, আজ $day।")

            return
        }

        // =========================
        // GOOGLE SEARCH
        // =========================

        if (
            command.startsWith("গুগলে সার্চ") ||
            command.startsWith("google search")
        ) {

            val query =
                command
                    .replace("গুগলে সার্চ", "")
                    .replace("google search", "")
                    .trim()

            if (query.isNotEmpty()) {

                speak("জি স্যার, গুগলে সার্চ করছি।")

                openDelayed {

                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://www.google.com/search?q=" +
                                    Uri.encode(query)
                        )
                    )
                }

            } else {

                speak("স্যার, কী সার্চ করব?")
            }

            return
        }

        // =========================
        // YOUTUBE SEARCH
        // =========================

        if (
            command.startsWith("ইউটিউবে সার্চ") ||
            command.startsWith("youtube search")
        ) {

            val query =
                command
                    .replace("ইউটিউবে সার্চ", "")
                    .replace("youtube search", "")
                    .trim()

            if (query.isNotEmpty()) {

                speak("জি স্যার, ইউটিউবে সার্চ করছি।")

                openDelayed {

                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://www.youtube.com/results?search_query=" +
                                    Uri.encode(query)
                        )
                    )
                }

            } else {

                speak("স্যার, কী সার্চ করব?")
            }

            return
        }

        // =========================
        // INSTALLED APP OPEN
        // =========================

        if (openInstalledApp(command)) {
            return
        }

        // =========================
        // BASIC AI-LIKE RESPONSES
        // =========================

        answerLocal(command)
    }

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
                .replace("খোলো", "")
                .replace("খুলে দাও", "")
                .replace("চালু কর", "")
                .replace("চালু করে দাও", "")
                .replace("open", "")
                .replace("launch", "")
                .trim()

        if (requested.isEmpty()) {
            return false
        }

        var bestLabel: String? = null
        var bestIntent: Intent? = null

        for (info in apps) {

            val label =
                info.loadLabel(pm)
                    .toString()
                    .lowercase(Locale("bn", "BD"))

            if (
                label == requested ||
                label.contains(requested) ||
                requested.contains(label)
            ) {

                val launchIntent =
                    pm.getLaunchIntentForPackage(
                        info.activityInfo.packageName
                    )

                if (launchIntent != null) {

                    bestLabel = label
                    bestIntent = launchIntent

                    break
                }
            }
        }

        if (bestIntent != null) {

            speak(
                "জি স্যার, $bestLabel খুলছি।"
            )

            handler.postDelayed({

                bestIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                try {
                    startActivity(bestIntent)
                } catch (_: Exception) {
                    speak(
                        "স্যার, অ্যাপটি খোলা যাচ্ছে না।"
                    )
                }

            }, 1000)

            return true
        }

        return false
    }

    private fun setFlash(enable: Boolean) {

        try {

            val cameraManager =
                getSystemService(
                    CAMERA_SERVICE
                ) as CameraManager

            var cameraId: String? = null

            for (id in cameraManager.cameraIdList) {

                val characteristics =
                    cameraManager.getCameraCharacteristics(id)

                val hasFlash =
                    characteristics.get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true

                val facing =
                    characteristics.get(
                        CameraCharacteristics.LENS_FACING
                    )

                if (
                    hasFlash &&
                    facing ==
                    CameraCharacteristics.LENS_FACING_BACK
                ) {

                    cameraId = id
                    break
                }
            }

            if (cameraId == null) {

                speak(
                    "স্যার, এই ফোনে ফ্ল্যাশ পাওয়া যাচ্ছে না।"
                )

                return
            }

            cameraManager.setTorchMode(
                cameraId,
                enable
            )

            if (enable) {

                speak(
                    "জি স্যার, ফ্ল্যাশ চালু করেছি।"
                )

            } else {

                speak(
                    "জি স্যার, ফ্ল্যাশ বন্ধ করেছি।"
                )
            }

        } catch (_: Exception) {

            speak(
                "স্যার, ফ্ল্যাশ নিয়ন্ত্রণ করা যাচ্ছে না।"
            )
        }
    }

    private fun openDelayed(
        provider: () -> Intent
    ) {

        handler.postDelayed({

            try {

                val intent = provider()

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

            } catch (_: Exception) {

                speak(
                    "স্যার, এই কাজটি এখন করা যাচ্ছে না।"
                )
            }

        }, 900)
    }

    private fun answerLocal(
        command: String
    ) {

        when {

            command.contains("তুমি কে") ->

                speak(
                    "আমি রাজ্য AI, আপনার বাংলা ভয়েস অ্যাসিস্ট্যান্ট।"
                )

            command.contains("তোমার নাম") ->

                speak(
                    "আমার নাম রাজ্য AI।"
                )

            command.contains("হ্যালো") ||
            command.contains("হাই") ||
            command.contains("hello") ->

                speak(
                    "হ্যালো স্যার। বলুন, কী করতে পারি?"
                )

            command.contains("ধন্যবাদ") ->

                speak(
                    "স্বাগতম স্যার।"
                )

            else ->

                speak(
                    "স্যার, এই কমান্ডটি এখনো আমার কাজের তালিকায় যোগ করা হয়নি।"
                )
        }
    }

    private fun normalize(
        text: String
    ): String {

        return text
            .trim()
            .lowercase(Locale("bn", "BD"))
            .replace(Regex("\\s+"), " ")
    }

    private fun speak(
        text: String
    ) {

        speaking = true

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "rajjo_${System.currentTimeMillis()}"
        )

        handler.postDelayed({

            speaking = false

            startListeningAfterDelay(400)

        }, max(2200, text.length * 70L))
    }

    private fun speakAndStop(
        text: String
    ) {

        speaking = true

        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "rajjo_stop"
        )

        handler.postDelayed({

            stopSelf()

        }, max(2200, text.length * 70L))
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
    ): IBinder? = null
}
