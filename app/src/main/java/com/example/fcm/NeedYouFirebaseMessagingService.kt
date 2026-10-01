package com.example.fcm

import android.util.Log
import com.example.data.FirebaseManager
import com.example.data.PreferencesManager
import com.example.model.PingType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NeedYouFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        FirebaseManager.getInstance(applicationContext).updateFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val pingTypeStr = data["type"] ?: data["ping_type"] ?: remoteMessage.notification?.title
        val senderName = data["from_name"] ?: data["sender_name"] ?: "Your friend"

        val pingType = PingType.fromString(pingTypeStr)
        val prefs = PreferencesManager(applicationContext)

        NotificationHelper.showPingNotification(
            context = applicationContext,
            pingType = pingType,
            senderName = senderName,
            soundPreset = prefs.getSavedSound(),
            vibrationEnabled = prefs.getSavedVibration()
        )
    }

    companion object {
        private const val TAG = "NeedYouFCM"
    }
}
