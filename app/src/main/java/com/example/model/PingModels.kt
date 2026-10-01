package com.example.model

enum class PingType(
    val title: String,
    val iconEmoji: String,
    val notificationFormat: String,
    val defaultSoundKey: String,
    val vibrationPattern: LongArray
) {
    NEED_YOU(
        title = "I NEED YOU",
        iconEmoji = "🔔",
        notificationFormat = "%s needs you right now.",
        defaultSoundKey = "urgent_ping",
        vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
    ),
    BORED(
        title = "HOW'S YOUR DAY?",
        iconEmoji = "👋",
        notificationFormat = "%s: Hi, how is your day going? 😊",
        defaultSoundKey = "bored_ping",
        vibrationPattern = longArrayOf(0, 200, 100, 200)
    );

    fun formatMessage(senderName: String): String {
        val safeName = senderName.ifBlank { "Your friend" }
        return String.format(notificationFormat, safeName)
    }

    companion object {
        fun fromString(value: String?): PingType {
            return when (value?.uppercase()) {
                "BORED", "IM_BORED", "I_M_BORED", "DAY_CHECK", "HOWS_YOUR_DAY", "HOW_IS_YOUR_DAY" -> BORED
                else -> NEED_YOU
            }
        }
    }
}

enum class SoundPreset(
    val id: String,
    val displayName: String,
    val rawResName: String?
) {
    URGENT("urgent_ping", "Urgent Alert Chime", "urgent_ping"),
    BORED("bored_ping", "Playful Chime", "bored_ping"),
    SOFT_BELL("soft_bell", "Harmonic Soft Bell", "soft_bell"),
    RADAR("radar_ping", "Radar Sonar Pulse", "radar_ping"),
    SYSTEM("system_default", "System Default Tone", null);

    companion object {
        fun fromId(id: String?): SoundPreset {
            return entries.find { it.id == id } ?: URGENT
        }
    }
}

data class PingRecord(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val type: PingType = PingType.NEED_YOU,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class UserProfile(
    val userId: String = "",
    val displayName: String = "Friend",
    val pairCode: String = "",
    val pairedWithUserId: String? = null,
    val pairedWithName: String? = null,
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = 0L,
    val fcmToken: String? = null
) {
    val isPaired: Boolean get() = !pairedWithUserId.isNullOrBlank()
}

enum class FriendStatus {
    CONNECTED,
    OFFLINE
}
