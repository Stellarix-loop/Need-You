package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SoundPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("need_you_preferences", Context.MODE_PRIVATE)

    private val _displayName = MutableStateFlow(getSavedDisplayName())
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(getSavedVibration())
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _soundPreset = MutableStateFlow(getSavedSound())
    val soundPreset: StateFlow<SoundPreset> = _soundPreset.asStateFlow()

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun getUserId(): String {
        var id = prefs.getString(KEY_USER_ID, null)
        if (id.isNullOrBlank()) {
            id = "user_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString(KEY_USER_ID, id).apply()
        }
        return id
    }

    fun getSavedDisplayName(): String {
        return prefs.getString(KEY_DISPLAY_NAME, "My Friend") ?: "My Friend"
    }

    fun setDisplayName(name: String) {
        val trimmed = name.trim().take(30)
        prefs.edit().putString(KEY_DISPLAY_NAME, trimmed).apply()
        _displayName.value = trimmed
    }

    fun getSavedVibration(): Boolean {
        return prefs.getBoolean(KEY_VIBRATION, true)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _vibrationEnabled.value = enabled
    }

    fun getSavedSound(): SoundPreset {
        val raw = prefs.getString(KEY_SOUND_PRESET, SoundPreset.URGENT.id)
        return SoundPreset.fromId(raw)
    }

    fun setSoundPreset(preset: SoundPreset) {
        prefs.edit().putString(KEY_SOUND_PRESET, preset.id).apply()
        _soundPreset.value = preset
    }

    fun getSavedThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "dark") ?: "dark"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun getPairCode(): String {
        var code = prefs.getString(KEY_PAIR_CODE, null)
        if (code.isNullOrBlank()) {
            val randomNum = (100000..999999).random()
            code = randomNum.toString()
            prefs.edit().putString(KEY_PAIR_CODE, code).apply()
        }
        return code
    }

    fun getCachedPairedFriendId(): String? = prefs.getString(KEY_PAIRED_FRIEND_ID, null)

    fun getCachedPairedFriendName(): String? = prefs.getString(KEY_PAIRED_FRIEND_NAME, null)

    fun setCachedPairing(friendId: String?, friendName: String?) {
        prefs.edit()
            .putString(KEY_PAIRED_FRIEND_ID, friendId)
            .putString(KEY_PAIRED_FRIEND_NAME, friendName)
            .apply()
    }

    companion object {
        private const val KEY_USER_ID = "pref_user_id"
        private const val KEY_DISPLAY_NAME = "pref_display_name"
        private const val KEY_VIBRATION = "pref_vibration"
        private const val KEY_SOUND_PRESET = "pref_sound_preset"
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_PAIR_CODE = "pref_pair_code"
        private const val KEY_PAIRED_FRIEND_ID = "pref_paired_friend_id"
        private const val KEY_PAIRED_FRIEND_NAME = "pref_paired_friend_name"
    }
}
