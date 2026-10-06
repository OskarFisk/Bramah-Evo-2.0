package com.brahma.connect.commands

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.brahma.connect.BuildConfig
import com.brahma.connect.core.BrahmaConnectCapabilities
import com.brahma.connect.core.CommandResult
import com.brahma.connect.device.AndroidDeviceInfoProvider

class DeviceCommandHandler(private val context: Context) {
    private val infoProvider = AndroidDeviceInfoProvider(context)

    fun handle(action: String, parameters: Map<String, Any?>): CommandResult {
        return when (action.lowercase()) {
            "get_device_info" -> getDeviceInfo()
            "get_battery" -> getBattery()
            "flashlight_on" -> flashlight(true)
            "flashlight_off" -> flashlight(false)
            "launch_app" -> launchApp(parameters)
            "open_url" -> openUrl(parameters)
            "volume_get" -> volumeGet()
            "volume_set" -> volumeSet(parameters)
            "list_apps" -> listApps(parameters)
            "share_text" -> shareText(parameters)
            "compose_sms" -> composeMessage(parameters, "sms")
            "compose_email" -> composeMessage(parameters, "email")
            "open_map" -> openMap(parameters)
            "create_calendar_event" -> createCalendarEvent(parameters)
            "set_alarm" -> setAlarm(parameters)
            "open_settings" -> openSettings(parameters)
            "unlock_phone" -> unlockPhone(parameters)
            "file_list" -> fileList(parameters)
            "file_read" -> fileRead(parameters)
            "file_write" -> fileWrite(parameters)
            "file_delete" -> fileDelete(parameters)
            "ui_dump" -> uiDump()
            "ui_tap" -> uiTap(parameters)
            "ui_swipe" -> uiSwipe(parameters)
            "ui_type" -> uiType(parameters)
            else -> CommandResult(false, errorCode = "UNKNOWN_COMMAND", error = "Unsupported command: $action")
        }
    }

    private fun getDeviceInfo(): CommandResult {
        val battery = infoProvider.batterySnapshot()
        return CommandResult(
            success = true,
            data = mapOf(
                "device_id" to null,
                "device_name" to (Build.MODEL ?: "Android"),
                "platform" to "android",
                "android_version" to (Build.VERSION.RELEASE ?: "Unknown"),
                "agent_version" to BuildConfig.VERSION_NAME,
                "model" to (Build.MODEL ?: "Android"),
                "battery" to battery.first,
                "charging" to battery.second,
                "wifi_state" to if (infoProvider.wifiEnabled()) "connected" else "disconnected",
                "capabilities" to BrahmaConnectCapabilities.INITIAL,
            ),
        )
    }

    private fun getBattery(): CommandResult {
        val battery = infoProvider.batterySnapshot()
        return CommandResult(true, data = mapOf("percentage" to battery.first, "charging" to battery.second))
    }

    private fun flashlight(enabled: Boolean): CommandResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return CommandResult(
                false,
                errorCode = "CAMERA_PERMISSION_REQUIRED",
                error = "Camera permission is required to control the flashlight.",
            )
        }
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CommandResult(false, errorCode = "FLASHLIGHT_UNAVAILABLE", error = "Camera service unavailable.")
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            runCatching {
                val chars = manager.getCameraCharacteristics(id)
                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                flash && (facing == CameraCharacteristics.LENS_FACING_BACK || facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
            }.getOrDefault(false)
        } ?: return CommandResult(false, errorCode = "FLASHLIGHT_UNAVAILABLE", error = "No flashlight on this device.")
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val maxStrength = runCatching {
                    manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                }.getOrDefault(1)
                if (enabled && maxStrength > 1) {
                    manager.turnOnTorchWithStrengthLevel(cameraId, maxStrength)
                } else {
                    manager.setTorchMode(cameraId, enabled)
                }
            } else {
                manager.setTorchMode(cameraId, enabled)
            }
            CommandResult(true, data = mapOf("flashlight" to if (enabled) "on" else "off"))
        } catch (security: SecurityException) {
            CommandResult(false, errorCode = "FLASHLIGHT_UNAVAILABLE", error = security.message ?: "Flashlight permission denied.")
        } catch (exc: Exception) {
            CommandResult(false, errorCode = "FLASHLIGHT_UNAVAILABLE", error = exc.message ?: "Flashlight unavailable.")
        }
    }

    private fun launchApp(parameters: Map<String, Any?>): CommandResult {
        val requested = listOf("package", "package_name", "app_name", "app", "name")
            .firstNotNullOfOrNull { key -> parameters[key]?.toString()?.trim() }
            .orEmpty()
        if (requested.isBlank()) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "App name or package name is required.")
        }

        val packageName = resolveLaunchablePackage(requested)
            ?: requested.takeIf { context.packageManager.getLaunchIntentForPackage(it) != null }
            ?: return CommandResult(false, errorCode = "APP_NOT_FOUND", error = "Package not found: $requested")

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return CommandResult(false, errorCode = "APP_NOT_FOUND", error = "Package not found: $packageName")
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult(true, data = mapOf("package" to packageName, "app_name" to requested))
        } catch (exc: Exception) {
            CommandResult(false, errorCode = "APP_NOT_FOUND", error = exc.message ?: "Unable to launch app.")
        }
    }

    private fun resolveLaunchablePackage(requested: String): String? {
        val normalizedRequested = normalizeAppQuery(requested)
        if (normalizedRequested.isBlank()) return null

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchableApps = runCatching {
            context.packageManager.queryIntentActivities(launcherIntent, 0)
        }.getOrDefault(emptyList())

        fun labelOf(info: ResolveInfo): String {
            return info.loadLabel(context.packageManager)?.toString().orEmpty()
        }

        val exactMatch = launchableApps.firstOrNull { info ->
            val label = normalizeAppQuery(labelOf(info))
            val packageId = normalizeAppQuery(info.activityInfo.packageName)
            normalizedRequested == label || normalizedRequested == packageId
        }
        if (exactMatch != null) return exactMatch.activityInfo.packageName

        val containsMatch = launchableApps.firstOrNull { info ->
            val label = normalizeAppQuery(labelOf(info))
            val packageId = normalizeAppQuery(info.activityInfo.packageName)
            label.contains(normalizedRequested) ||
                normalizedRequested.contains(label) ||
                packageId.contains(normalizedRequested) ||
                normalizedRequested.contains(packageId)
        }
        return containsMatch?.activityInfo?.packageName
    }

    private fun normalizeAppQuery(value: String): String {
        return value.lowercase().replace(Regex("[^a-z0-9]+"), "")
    }

    private fun openUrl(parameters: Map<String, Any?>): CommandResult {
        val url = (parameters["url"] ?: parameters["link"])?.toString()?.trim().orEmpty()
        if (url.isBlank()) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "URL is required.")
        }
        val uri = Uri.parse(url)
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            return CommandResult(
                false,
                errorCode = "INVALID_ARGUMENT",
                error = "Only valid http and https URLs can be opened.",
            )
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult(true, data = mapOf("url" to url))
        } catch (exc: Exception) {
            CommandResult(false, errorCode = "OPEN_URL_FAILED", error = exc.message ?: "Unable to open URL.")
        }
    }

    private fun volumeGet(): CommandResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return CommandResult(false, errorCode = "AUDIO_UNAVAILABLE", error = "Audio service unavailable.")
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val percentage = ((current * 100f) / max).toInt().coerceIn(0, 100)
        return CommandResult(true, data = mapOf("stream" to "music", "percentage" to percentage, "current" to current, "max" to max))
    }

    private fun volumeSet(parameters: Map<String, Any?>): CommandResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return CommandResult(false, errorCode = "AUDIO_UNAVAILABLE", error = "Audio service unavailable.")
        val targetRaw = parameters["value"] ?: parameters["percentage"]
            ?: return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "Volume value is required.")
        val target = when (targetRaw) {
            is Number -> targetRaw.toInt()
            else -> targetRaw.toString().toIntOrNull()
        } ?: return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "Volume value must be numeric.")
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = target.coerceIn(0, 100)
        val level = ((clamped / 100f) * max).toInt().coerceIn(0, max)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        return CommandResult(true, data = mapOf("stream" to "music", "percentage" to clamped, "current" to level, "max" to max))
    }

    private fun listApps(parameters: Map<String, Any?>): CommandResult {
        val query = parameters["query"]?.toString()?.trim().orEmpty()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = context.packageManager.queryIntentActivities(launcherIntent, 0)
            .map { info ->
                mapOf(
                    "name" to info.loadLabel(context.packageManager).toString(),
                    "package" to info.activityInfo.packageName,
                )
            }
            .distinctBy { it["package"] }
            .filter { query.isBlank() || it["name"].toString().contains(query, ignoreCase = true) }
            .sortedBy { it["name"].toString().lowercase() }
            .take(100)
        return CommandResult(true, data = mapOf("apps" to apps, "count" to apps.size))
    }

    private fun shareText(parameters: Map<String, Any?>): CommandResult {
        val text = parameters["text"]?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "Text to share is required.")
        }
        return launchUserAction(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text),
            "SHARE_UNAVAILABLE",
            "No app is available to share this text.",
        )
    }

    private fun composeMessage(parameters: Map<String, Any?>, kind: String): CommandResult {
        val recipient = parameters["recipient"]?.toString()?.trim().orEmpty()
        val body = parameters["body"]?.toString()?.trim().orEmpty()
        val subject = parameters["subject"]?.toString()?.trim().orEmpty()
        if (body.isBlank() && recipient.isBlank()) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "A recipient or message body is required.")
        }
        val uri = Uri.fromParts(if (kind == "sms") "smsto" else "mailto", recipient, null)
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            if (body.isNotBlank()) putExtra(if (kind == "sms") "sms_body" else Intent.EXTRA_TEXT, body)
            if (subject.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchUserAction(
            intent,
            "${kind.uppercase()}_UNAVAILABLE",
            "No app is available to compose this ${if (kind == "sms") "message" else "email"}.",
        )
    }

    private fun openMap(parameters: Map<String, Any?>): CommandResult {
        val query = parameters["query"]?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "A place or address is required.")
        }
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        return launchUserAction(
            Intent(Intent.ACTION_VIEW, uri),
            "MAPS_UNAVAILABLE",
            "No map app is available to open this place.",
        )
    }

    private fun createCalendarEvent(parameters: Map<String, Any?>): CommandResult {
        val title = parameters["title"]?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "An event title is required.")
        }
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, title)
            parameters["description"]?.toString()?.takeIf(String::isNotBlank)?.let {
                putExtra(CalendarContract.Events.DESCRIPTION, it)
            }
            parameters["location"]?.toString()?.takeIf(String::isNotBlank)?.let {
                putExtra(CalendarContract.Events.EVENT_LOCATION, it)
            }
            (parameters["begin_time"] as? Number)?.toLong()?.let {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
            }
            (parameters["end_time"] as? Number)?.toLong()?.let {
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchUserAction(intent, "CALENDAR_UNAVAILABLE", "No calendar app is available.")
    }

    private fun setAlarm(parameters: Map<String, Any?>): CommandResult {
        val hour = (parameters["hour"] as? Number)?.toInt()
            ?: parameters["hour"]?.toString()?.toIntOrNull()
        val minute = (parameters["minute"] as? Number)?.toInt()
            ?: parameters["minute"]?.toString()?.toIntOrNull()
        if (hour == null || hour !in 0..23 || minute == null || minute !in 0..59) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "Use an hour from 0 to 23 and a minute from 0 to 59.")
        }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            parameters["label"]?.toString()?.takeIf(String::isNotBlank)?.let {
                putExtra(AlarmClock.EXTRA_MESSAGE, it)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchUserAction(intent, "ALARM_UNAVAILABLE", "No clock app is available to set an alarm.")
    }

    private fun openSettings(parameters: Map<String, Any?>): CommandResult {
        val action = when (parameters["section"]?.toString()?.lowercase()?.trim()) {
            "wifi", "network" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "app", "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> return CommandResult(
                false,
                errorCode = "INVALID_ARGUMENT",
                error = "Choose one of: wifi, bluetooth, display, sound, or apps.",
            )
        }
        return launchUserAction(
            Intent(action),
            "SETTINGS_UNAVAILABLE",
            "This settings screen is not available on this device.",
        )
    }

    private fun launchUserAction(intent: Intent, errorCode: String, error: String): CommandResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            val userIntent = if (intent.action == Intent.ACTION_SEND) {
                Intent.createChooser(intent, "Share with").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                intent
            }
            context.startActivity(userIntent)
            CommandResult(true, data = mapOf("opened" to true))
        } catch (exc: Exception) {
            CommandResult(false, errorCode = errorCode, error = exc.message ?: error)
        }
    }

    private fun unlockPhone(parameters: Map<String, Any?>): CommandResult {
        val pin = parameters["pin"]?.toString()
        if (pin.isNullOrBlank()) {
            return CommandResult(false, errorCode = "MISSING_PIN", error = "PIN is required to unlock phone.")
        }
        val service = com.brahma.connect.accessibility.BrahmaAccessibilityService.instance
        if (service == null) {
            return CommandResult(false, errorCode = "ACCESSIBILITY_DISABLED", error = "Brahma Accessibility Service is not enabled.")
        }
        val success = service.unlockPhone(pin)
        return if (success) {
            CommandResult(true, data = mapOf("message" to "Unlock sequence initiated."))
        } else {
            CommandResult(false, errorCode = "UNLOCK_FAILED", error = "Failed to initiate unlock sequence.")
        }
    }

    private fun resolveFileTarget(path: String?): java.io.File {
        val storage = android.os.Environment.getExternalStorageDirectory()
        if (path.isNullOrBlank() || path.equals("home", ignoreCase = true)) {
            return storage
        }
        val lower = path.lowercase().trim()
        val baseDir = when (lower) {
            "downloads" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            "documents" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
            "pictures" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
            "music" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            "movies", "videos" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
            else -> java.io.File(storage, path)
        }
        return baseDir
    }

    private fun fileList(parameters: Map<String, Any?>): CommandResult {
        val path = parameters["path"]?.toString()
        val dir = resolveFileTarget(path)
        if (!dir.exists() || !dir.isDirectory) {
            return CommandResult(false, errorCode = "NOT_FOUND", error = "Directory not found: ${dir.absolutePath}")
        }
        val items = dir.listFiles()?.map { file ->
            mapOf(
                "name" to file.name,
                "is_dir" to file.isDirectory,
                "size" to file.length(),
                "path" to file.absolutePath
            )
        } ?: emptyList()
        return CommandResult(true, data = mapOf("items" to items, "path" to dir.absolutePath))
    }

    private fun fileRead(parameters: Map<String, Any?>): CommandResult {
        val path = parameters["path"]?.toString()
        val file = resolveFileTarget(path)
        if (!file.exists() || !file.isFile) {
            return CommandResult(false, errorCode = "NOT_FOUND", error = "File not found: ${file.absolutePath}")
        }
        return try {
            val content = file.readText()
            CommandResult(true, data = mapOf("content" to content, "path" to file.absolutePath))
        } catch (e: Exception) {
            CommandResult(false, errorCode = "READ_ERROR", error = e.message ?: "Failed to read file.")
        }
    }

    private fun fileWrite(parameters: Map<String, Any?>): CommandResult {
        val path = parameters["path"]?.toString()
        val content = parameters["content"]?.toString() ?: ""
        val append = parameters["append"] as? Boolean ?: false
        val file = resolveFileTarget(path)
        return try {
            file.parentFile?.mkdirs()
            if (append) {
                file.appendText(content)
            } else {
                file.writeText(content)
            }
            CommandResult(true, data = mapOf("message" to "File written successfully.", "path" to file.absolutePath))
        } catch (e: Exception) {
            CommandResult(false, errorCode = "WRITE_ERROR", error = e.message ?: "Failed to write file.")
        }
    }

    private fun fileDelete(parameters: Map<String, Any?>): CommandResult {
        val path = parameters["path"]?.toString()
        val file = resolveFileTarget(path)
        if (!file.exists()) {
            return CommandResult(false, errorCode = "NOT_FOUND", error = "File or directory not found: ${file.absolutePath}")
        }
        return try {
            val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (success) {
                CommandResult(true, data = mapOf("message" to "Deleted successfully.", "path" to file.absolutePath))
            } else {
                CommandResult(false, errorCode = "DELETE_FAILED", error = "Failed to delete.")
            }
        } catch (e: Exception) {
            CommandResult(false, errorCode = "DELETE_ERROR", error = e.message ?: "Failed to delete file.")
        }
    }

    private fun uiDump(): CommandResult {
        val service = com.brahma.connect.accessibility.BrahmaAccessibilityService.instance
        if (service == null) {
            return CommandResult(false, errorCode = "ACCESSIBILITY_DISABLED", error = "Brahma Accessibility Service is not enabled.")
        }
        val result = service.dumpUiTree()
        return if (result.containsKey("error")) {
            CommandResult(false, errorCode = "UI_DUMP_FAILED", error = result["error"] as String)
        } else {
            CommandResult(true, data = result)
        }
    }

    private fun uiTap(parameters: Map<String, Any?>): CommandResult {
        val service = com.brahma.connect.accessibility.BrahmaAccessibilityService.instance
        if (service == null) {
            return CommandResult(false, errorCode = "ACCESSIBILITY_DISABLED", error = "Brahma Accessibility Service is not enabled.")
        }
        val x = (parameters["x"] as? Number)?.toInt()
        val y = (parameters["y"] as? Number)?.toInt()
        if (x == null || y == null) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "x and y coordinates are required.")
        }
        val success = service.tap(x, y)
        return CommandResult(success, data = mapOf("action" to "tap", "success" to success))
    }

    private fun uiSwipe(parameters: Map<String, Any?>): CommandResult {
        val service = com.brahma.connect.accessibility.BrahmaAccessibilityService.instance
        if (service == null) {
            return CommandResult(false, errorCode = "ACCESSIBILITY_DISABLED", error = "Brahma Accessibility Service is not enabled.")
        }
        val x1 = (parameters["x1"] as? Number)?.toInt()
        val y1 = (parameters["y1"] as? Number)?.toInt()
        val x2 = (parameters["x2"] as? Number)?.toInt()
        val y2 = (parameters["y2"] as? Number)?.toInt()
        val duration = (parameters["duration"] as? Number)?.toLong() ?: 300L
        if (x1 == null || y1 == null || x2 == null || y2 == null) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "x1, y1, x2, y2 coordinates are required.")
        }
        val success = service.swipe(x1, y1, x2, y2, duration)
        return CommandResult(success, data = mapOf("action" to "swipe", "success" to success))
    }

    private fun uiType(parameters: Map<String, Any?>): CommandResult {
        val service = com.brahma.connect.accessibility.BrahmaAccessibilityService.instance
        if (service == null) {
            return CommandResult(false, errorCode = "ACCESSIBILITY_DISABLED", error = "Brahma Accessibility Service is not enabled.")
        }
        val text = parameters["text"]?.toString()
        if (text.isNullOrEmpty()) {
            return CommandResult(false, errorCode = "INVALID_ARGUMENT", error = "text is required.")
        }
        val success = service.typeText(text)
        return CommandResult(success, data = mapOf("action" to "type", "success" to success))
    }
}
