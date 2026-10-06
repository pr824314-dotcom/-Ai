private fun openInstalledApp(command: String): Boolean {

    val pm = packageManager

    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val apps = pm.queryIntentActivities(
        launcherIntent,
        PackageManager.MATCH_ALL
    )

    var requested = command
        .lowercase(Locale("bn", "BD"))
        .trim()

    // কমান্ড থেকে সাধারণ action words বাদ
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

    // বাংলা ও ইংরেজি অ্যাপ নামের alias
    val aliases = mapOf(
        "ফেসবুক" to listOf("facebook"),
        "এফবি" to listOf("facebook"),
        "fb" to listOf("facebook"),

        "ইউটিউব" to listOf("youtube"),
        "youtube" to listOf("youtube"),

        "হোয়াটসঅ্যাপ" to listOf("whatsapp"),
        "হোয়াটসঅ্যাপ" to listOf("whatsapp"),
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

        "প্লে স্টোর" to listOf("play store", "google play"),
        "play store" to listOf("play store", "google play")
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

                    }, 900)

                    return true
                }
            }
        }
    }

    return false
}
