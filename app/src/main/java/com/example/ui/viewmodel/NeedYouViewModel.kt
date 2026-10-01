package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseManager
import com.example.data.PreferencesManager
import com.example.fcm.NotificationHelper
import com.example.model.FriendStatus
import com.example.model.PingRecord
import com.example.model.PingType
import com.example.model.SoundPreset
import com.example.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NeedYouViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val firebaseManager = FirebaseManager.getInstance(application)

    val userProfile: StateFlow<UserProfile> = firebaseManager.userProfile
    val friendStatus: StateFlow<FriendStatus> = firebaseManager.friendStatus
    val friendLastSeenTimestamp: StateFlow<Long> = firebaseManager.friendLastSeenTimestamp
    val recentPings: StateFlow<List<PingRecord>> = firebaseManager.recentPings
    val isLiveFirebaseMode: StateFlow<Boolean> = firebaseManager.isLiveFirebaseMode

    val displayName: StateFlow<String> = prefs.displayName
    val vibrationEnabled: StateFlow<Boolean> = prefs.vibrationEnabled
    val soundPreset: StateFlow<SoundPreset> = prefs.soundPreset
    val themeMode: StateFlow<String> = prefs.themeMode

    // Anti-spam cooldown state
    private val _cooldownSecondsRemaining = MutableStateFlow(0)
    val cooldownSecondsRemaining: StateFlow<Int> = _cooldownSecondsRemaining.asStateFlow()

    private var cooldownJob: Job? = null

    // UI Feedback state
    private val _pingSentFeedback = MutableStateFlow<String?>(null)
    val pingSentFeedback: StateFlow<String?> = _pingSentFeedback.asStateFlow()

    private val _isSendingPing = MutableStateFlow(false)
    val isSendingPing: StateFlow<Boolean> = _isSendingPing.asStateFlow()

    private val _isPairingInProgress = MutableStateFlow(false)
    val isPairingInProgress: StateFlow<Boolean> = _isPairingInProgress.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _activeInAppPing = MutableStateFlow<PingRecord?>(null)
    val activeInAppPing: StateFlow<PingRecord?> = _activeInAppPing.asStateFlow()

    init {
        // Listen for real-time incoming pings while app is in foreground
        viewModelScope.launch {
            firebaseManager.incomingPings.collect { ping ->
                _activeInAppPing.value = ping
                if (vibrationEnabled.value) {
                    NotificationHelper.triggerVibration(
                        getApplication(),
                        ping.type.vibrationPattern
                    )
                }
                NotificationHelper.previewSound(getApplication(), soundPreset.value)
            }
        }
    }

    fun sendPing(type: PingType) {
        if (_cooldownSecondsRemaining.value > 0 || _isSendingPing.value) return

        viewModelScope.launch {
            _isSendingPing.value = true
            val result = firebaseManager.sendPing(type)
            _isSendingPing.value = false

            result.onSuccess {
                // Trigger local sender vibration feedback
                if (vibrationEnabled.value) {
                    NotificationHelper.triggerVibration(
                        getApplication(),
                        longArrayOf(0, 100, 50, 100)
                    )
                }
                _pingSentFeedback.value = "Ping sent ✓"
                startCooldown(COOLDOWN_DURATION_SECONDS)

                // Auto clear feedback after 3.5s
                viewModelScope.launch {
                    delay(3500)
                    if (_pingSentFeedback.value == "Ping sent ✓") {
                        _pingSentFeedback.value = null
                    }
                }
            }.onFailure { e ->
                _userMessage.value = e.localizedMessage ?: "Failed to send ping."
            }
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _cooldownSecondsRemaining.value = seconds
            for (i in (seconds - 1) downTo 0) {
                delay(1000)
                _cooldownSecondsRemaining.value = i
            }
        }
    }

    fun pairWithCode(code: String, onSuccess: () -> Unit = {}) {
        if (_isPairingInProgress.value) return
        viewModelScope.launch {
            _isPairingInProgress.value = true
            val result = firebaseManager.pairWithCode(code)
            _isPairingInProgress.value = false

            result.onSuccess { friendName ->
                _userMessage.value = "Connected with $friendName! 🎉"
                onSuccess()
            }.onFailure { error ->
                _userMessage.value = error.localizedMessage ?: "Could not pair. Check the code."
            }
        }
    }

    fun unpairFriend(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            firebaseManager.unpairFriend()
            _userMessage.value = "Unpaired successfully."
            onComplete()
        }
    }

    fun updateDisplayName(name: String) {
        firebaseManager.updateDisplayName(name)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.setVibrationEnabled(enabled)
        if (enabled) {
            NotificationHelper.triggerVibration(getApplication(), longArrayOf(0, 120))
        }
    }

    fun setSoundPreset(preset: SoundPreset) {
        prefs.setSoundPreset(preset)
        previewSound(preset)
    }

    fun setThemeMode(mode: String) {
        prefs.setThemeMode(mode)
    }

    fun previewSound(preset: SoundPreset) {
        NotificationHelper.previewSound(getApplication(), preset)
    }

    fun triggerTestNotification() {
        val currentPreset = soundPreset.value
        val vibrate = vibrationEnabled.value
        val name = displayName.value
        NotificationHelper.showPingNotification(
            context = getApplication(),
            pingType = PingType.NEED_YOU,
            senderName = "$name (Test)",
            soundPreset = currentPreset,
            vibrationEnabled = vibrate
        )
        _userMessage.value = "Test notification sent! Check your notification bar."
    }

    fun toggleFriendStatusForDemo() {
        firebaseManager.toggleFriendStatusForDemo()
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    fun dismissInAppPing() {
        _activeInAppPing.value = null
    }

    companion object {
        const val COOLDOWN_DURATION_SECONDS = 15
    }
}
