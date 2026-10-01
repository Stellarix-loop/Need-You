package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PingType
import com.example.ui.theme.OnlineGreen
import com.example.ui.viewmodel.NeedYouViewModel

@Composable
fun PingButtonsSection(
    onSendPing: (PingType) -> Unit,
    cooldownSecondsRemaining: Int,
    pingSentFeedback: String?,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    val isCooldownActive = cooldownSecondsRemaining > 0

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Sleek "Ping Delivered" Status Pill
        AnimatedVisibility(
            visible = pingSentFeedback != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("ping_sent_feedback"),
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF0F1713),
                border = BorderStroke(1.dp, OnlineGreen.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(OnlineGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ping sent • Notifying friend",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnlineGreen,
                            letterSpacing = 0.3.sp
                        )
                    )
                }
            }
        }

        // 1. Primary Action: I NEED YOU
        LuxuryPingCard(
            title = "I NEED YOU",
            microLabel = "CRITICAL ALERT",
            description = "Triggers immediate sound & distinct vibration",
            icon = Icons.Default.NotificationsActive,
            themeGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFE51D4B),
                    Color(0xFFFF3366),
                    Color(0xFFFF5277)
                )
            ),
            glowColor = Color(0x35FF2A55),
            badgeColor = Color(0xFF990E30),
            isEnabled = !isCooldownActive && !isSending,
            isPulsing = !isCooldownActive,
            onClick = { onSendPing(PingType.NEED_YOU) },
            testTag = "need_you_button"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Secondary Action: HOW'S YOUR DAY?
        LuxuryPingCard(
            title = "HOW'S YOUR DAY?",
            microLabel = "DAILY CHECK-IN",
            description = "Sends: Hi, how is your day going? 😊",
            icon = Icons.Default.WavingHand,
            themeGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF4F46E5),
                    Color(0xFF6366F1),
                    Color(0xFF818CF8)
                )
            ),
            glowColor = Color(0x306366F1),
            badgeColor = Color(0xFF312E81),
            isEnabled = !isCooldownActive && !isSending,
            isPulsing = false,
            onClick = { onSendPing(PingType.BORED) },
            testTag = "bored_button"
        )

        // Engineered Cooldown Timer Indicator
        AnimatedVisibility(
            visible = isCooldownActive,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progress = cooldownSecondsRemaining.toFloat() / NeedYouViewModel.COOLDOWN_DURATION_SECONDS.toFloat()
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "READY IN ${cooldownSecondsRemaining}s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "• Cooldown prevents accidental spam",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LuxuryPingCard(
    title: String,
    microLabel: String,
    description: String,
    icon: ImageVector,
    themeGradient: Brush,
    glowColor: Color,
    badgeColor: Color,
    isEnabled: Boolean,
    isPulsing: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isEnabled) 0.965f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
        label = "btnScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .testTag(testTag)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            if (isEnabled) {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.32f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                    )
                )
            }
        ),
        shadowElevation = if (isEnabled) 12.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isEnabled) themeGradient
                    else Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .drawBehind {
                    if (isEnabled && isPulsing) {
                        drawCircle(
                            color = glowColor.copy(alpha = pulseAlpha),
                            radius = size.width * 0.45f
                        )
                    }
                }
                .padding(horizontal = 22.dp, vertical = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Category Chip / Tracking Label
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEnabled) Color.Black.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = microLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp,
                                fontSize = 10.sp,
                                color = if (isEnabled) Color.White.copy(alpha = 0.9f)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                            fontSize = 22.sp,
                            color = if (isEnabled) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isEnabled) Color.White.copy(alpha = 0.82f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            lineHeight = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Modern Icon Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isEnabled) Color.White.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    border = BorderStroke(
                        1.dp,
                        if (isEnabled) Color.White.copy(alpha = 0.35f)
                        else Color.Transparent
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isEnabled) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
