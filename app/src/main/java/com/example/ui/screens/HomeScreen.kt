package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.FriendStatus
import com.example.model.PingRecord
import com.example.model.PingType
import com.example.model.UserProfile
import com.example.ui.components.PingButtonsSection
import com.example.ui.components.PingHistoryList
import com.example.ui.components.StatusBadge
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.OfflineRed
import com.example.ui.theme.OnlineGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    friendStatus: FriendStatus,
    friendLastSeenTimestamp: Long = 0L,
    recentPings: List<PingRecord>,
    cooldownSecondsRemaining: Int,
    pingSentFeedback: String?,
    isSendingPing: Boolean,
    isPairingInProgress: Boolean,
    activeInAppPing: PingRecord?,
    onSendPing: (PingType) -> Unit,
    onPairWithCode: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismissInAppPing: () -> Unit,
    onToggleFriendStatus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Notification permission check for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Beacon animation for the live connection indicator
    val isOnline = friendStatus == FriendStatus.CONNECTED
    val transition = rememberInfiniteTransition(label = "pulse")
    val beaconPulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Need You",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp,
                                fontSize = 21.sp
                            )
                        )

                        if (userProfile.isPaired) {
                            Spacer(modifier = Modifier.width(12.dp))
                            StatusBadge(
                                status = friendStatus,
                                friendName = userProfile.pairedWithName
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("settings_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!userProfile.isPaired) {
                PairingScreen(
                    userProfile = userProfile,
                    isPairingInProgress = isPairingInProgress,
                    onPairWithCode = onPairWithCode
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Minimalist Notification Warning if needed
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1B1419),
                            border = BorderStroke(1.dp, Color(0x35FF2A55))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFFF2A55),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Enable notifications for instant alerts",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF2A55)
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Modern Minimalist Friend Presence Card with Visual Status Indicator
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friend_header_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Clean Minimalist Avatar
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isOnline) OnlineGreen.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = (userProfile.pairedWithName ?: "F").take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 18.sp,
                                                    color = if (isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = userProfile.pairedWithName ?: "Friend",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.3).sp,
                                                fontSize = 20.sp
                                            )
                                        )

                                        Text(
                                            text = "Connected friend",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }

                                // Interactive toggle for testing status changes in emulator/preview
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { onToggleFriendStatus() },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Toggle status",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Prominent Visual Connection Status Indicator Banner
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isOnline) OnlineGreen.copy(alpha = 0.08f)
                                else OfflineRed.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isOnline) OnlineGreen.copy(alpha = 0.25f)
                                    else OfflineRed.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("visual_connection_indicator")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Animated Beacon Dot
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        if (isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .scale(beaconPulseScale)
                                                    .background(OnlineGreen.copy(alpha = 0.35f), CircleShape)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .background(if (isOnline) OnlineGreen else OfflineRed, CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isOnline) "🟢 Online" else "🔴 Offline",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOnline) OnlineGreen else OfflineRed,
                                                    fontSize = 13.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isOnline) "• Active now" else "• Away",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }

                                        Text(
                                            text = if (isOnline) {
                                                "Friend is connected via Realtime Firestore"
                                            } else {
                                                "Friend is offline • Push alerts will wake their device"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // The Two Big Tactical Buttons (Minimalist & Modern)
                    PingButtonsSection(
                        onSendPing = onSendPing,
                        cooldownSecondsRemaining = cooldownSecondsRemaining,
                        pingSentFeedback = pingSentFeedback,
                        isSending = isSendingPing
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Activity Log
                    PingHistoryList(
                        pings = recentPings,
                        currentUserId = userProfile.userId
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // In-App Alert Banner (Flagship Dynamic Island Style)
            AnimatedVisibility(
                visible = activeInAppPing != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                activeInAppPing?.let { ping ->
                    val isNeedYou = ping.type == PingType.NEED_YOU
                    val accentGradient = if (isNeedYou) {
                        Brush.horizontalGradient(listOf(Color(0xFFE51D4B), Color(0xFFFF3366)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1)))
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_app_ping_banner"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        shadowElevation = 16.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(accentGradient)
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.25f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isNeedYou) Icons.Default.NotificationsActive else Icons.Default.Close,
                                            contentDescription = ping.type.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ping.type.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = ping.type.formatMessage(ping.fromUserName),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.9f),
                                            lineHeight = 16.sp
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = onDismissInAppPing,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
