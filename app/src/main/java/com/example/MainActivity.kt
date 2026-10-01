package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fcm.NotificationHelper
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NeedYouTheme
import com.example.ui.viewmodel.NeedYouViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NeedYouViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntentDeepLink(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "system" -> isSystemInDarkTheme()
                else -> true // Dark mode is default
            }

            NeedYouTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val friendStatus by viewModel.friendStatus.collectAsStateWithLifecycle()
                val friendLastSeen by viewModel.friendLastSeenTimestamp.collectAsStateWithLifecycle()
                val recentPings by viewModel.recentPings.collectAsStateWithLifecycle()
                val cooldownSeconds by viewModel.cooldownSecondsRemaining.collectAsStateWithLifecycle()
                val pingFeedback by viewModel.pingSentFeedback.collectAsStateWithLifecycle()
                val isSendingPing by viewModel.isSendingPing.collectAsStateWithLifecycle()
                val isPairing by viewModel.isPairingInProgress.collectAsStateWithLifecycle()
                val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
                val activeInAppPing by viewModel.activeInAppPing.collectAsStateWithLifecycle()

                val displayName by viewModel.displayName.collectAsStateWithLifecycle()
                val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
                val soundPreset by viewModel.soundPreset.collectAsStateWithLifecycle()
                val isLiveFirebase by viewModel.isLiveFirebaseMode.collectAsStateWithLifecycle()

                LaunchedEffect(userMessage) {
                    userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                        viewModel.dismissUserMessage()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                HomeScreen(
                                    userProfile = userProfile,
                                    friendStatus = friendStatus,
                                    friendLastSeenTimestamp = friendLastSeen,
                                    recentPings = recentPings,
                                    cooldownSecondsRemaining = cooldownSeconds,
                                    pingSentFeedback = pingFeedback,
                                    isSendingPing = isSendingPing,
                                    isPairingInProgress = isPairing,
                                    activeInAppPing = activeInAppPing,
                                    onSendPing = { viewModel.sendPing(it) },
                                    onPairWithCode = { code -> viewModel.pairWithCode(code) },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onDismissInAppPing = { viewModel.dismissInAppPing() },
                                    onToggleFriendStatus = { viewModel.toggleFriendStatusForDemo() }
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    userProfile = userProfile,
                                    displayName = displayName,
                                    vibrationEnabled = vibrationEnabled,
                                    soundPreset = soundPreset,
                                    themeMode = themeMode,
                                    isLiveFirebase = isLiveFirebase,
                                    onNavigateBack = { navController.popBackStack() },
                                    onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                                    onToggleVibration = { viewModel.setVibrationEnabled(it) },
                                    onSelectSoundPreset = { viewModel.setSoundPreset(it) },
                                    onPreviewSound = { viewModel.previewSound(it) },
                                    onSetThemeMode = { viewModel.setThemeMode(it) },
                                    onTriggerTestNotification = { viewModel.triggerTestNotification() },
                                    onUnpairFriend = { viewModel.unpairFriend() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentDeepLink(intent)
    }

    private fun handleIntentDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null) {
            val codeParam = data.getQueryParameter("code")
            if (!codeParam.isNullOrBlank() && codeParam.length == 6) {
                viewModel.pairWithCode(codeParam)
            }
        }

        // Tapped notification handling
        val pingTypeExtra = intent?.getStringExtra(NotificationHelper.EXTRA_PING_TYPE)
        val fromNameExtra = intent?.getStringExtra(NotificationHelper.EXTRA_FROM_NAME)
        if (!pingTypeExtra.isNullOrBlank()) {
            val sender = fromNameExtra ?: "Your friend"
            // Intent acknowledged
        }
    }
}
