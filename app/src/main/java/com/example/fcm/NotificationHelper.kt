package com.example.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.PingType
import com.example.model.SoundPreset

object NotificationHelper {

    const val CHANNEL_URGENT_ID = "need_you_urgent_alerts_v2"
    const val CHANNEL_BORED_ID = "need_you_bored_alerts_v2"
    const val CHANNEL_GENERAL_ID = "need_you_general_alerts_v2"

    const val EXTRA_PING_TYPE = "extra_ping_type"
    const val EXTRA_FROM_NAME = "extra_from_name"
    const val EXTRA_TIMESTAMP = "extra_timestamp"

    private var activeMediaPlayer: MediaPlayer? = null

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            // 1. Urgent Channel with custom sound and distinct urgent vibration
            val urgentSoundUri = getRawSoundUri(context, "urgent_ping")
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT_ID,
                "Need You Urgent Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alert pings when your friend urgently needs you"
                enableVibration(true)
                vibrationPattern = PingType.NEED_YOU.vibrationPattern
                setSound(urgentSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Daily Check-in Channel
            val boredSoundUri = getRawSoundUri(context, "bored_ping")
            val boredChannel = NotificationChannel(
                CHANNEL_BORED_ID,
                "Check-in Pings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily friendly check-in pings from your friend"
                enableVibration(true)
                vibrationPattern = PingType.BORED.vibrationPattern
                setSound(boredSoundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL_ID,
                "General Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Connection and pairing updates"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(urgentChannel, boredChannel, generalChannel)
            )
        }
    }

    fun showPingNotification(
        context: Context,
        pingType: PingType,
        senderName: String,
        soundPreset: SoundPreset = SoundPreset.URGENT,
        vibrationEnabled: Boolean = true
    ) {
        val message = pingType.formatMessage(senderName)
        val channelId = when (pingType) {
            PingType.NEED_YOU -> CHANNEL_URGENT_ID
            PingType.BORED -> CHANNEL_BORED_ID
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PING_TYPE, pingType.name)
            putExtra(EXTRA_FROM_NAME, senderName)
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getSoundUri(context, soundPreset)

        val title = when (pingType) {
            PingType.NEED_YOU -> "🚨 ${pingType.title}"
            PingType.BORED -> "👋 ${pingType.title}"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(0)

        if (soundUri != null) {
            builder.setSound(soundUri)
        }

        if (vibrationEnabled) {
            builder.setVibrate(pingType.vibrationPattern)
            triggerVibration(context, pingType.vibrationPattern)
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Android 13 permission not yet granted
        }
    }

    fun triggerVibration(context: Context, pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration errors if hardware unavailable
        }
    }

    fun previewSound(context: Context, preset: SoundPreset) {
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null

            val resId = getSoundResId(context, preset.rawResName)
            if (resId != 0) {
                activeMediaPlayer = MediaPlayer.create(context, resId)?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                            .build()
                    )
                    setOnCompletionListener { mp ->
                        mp.release()
                        if (activeMediaPlayer == mp) {
                            activeMediaPlayer = null
                        }
                    }
                    start()
                }
            } else {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notification)
                ringtone?.play()
            }
        } catch (_: Exception) {
            // Audio preview failed safely
        }
    }

    private fun getSoundUri(context: Context, preset: SoundPreset): Uri? {
        val rawRes = preset.rawResName
        return if (rawRes != null) {
            getRawSoundUri(context, rawRes)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    fun getRawSoundUri(context: Context, resourceName: String): Uri {
        return Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/$resourceName"
        )
    }

    private fun getSoundResId(context: Context, rawName: String?): Int {
        if (rawName.isNullOrBlank()) return 0
        return context.resources.getIdentifier(rawName, "raw", context.packageName)
    }
}
