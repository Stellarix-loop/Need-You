package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FriendStatus
import com.example.ui.theme.OfflineRed
import com.example.ui.theme.OnlineGreen

@Composable
fun StatusBadge(
    status: FriendStatus,
    friendName: String? = null,
    modifier: Modifier = Modifier
) {
    val isOnline = status == FriendStatus.CONNECTED

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier.testTag("status_badge"),
        shape = RoundedCornerShape(24.dp),
        color = if (isOnline) Color(0xFF0D1D16) else Color(0xFF1D1115),
        border = BorderStroke(
            1.dp,
            if (isOnline) OnlineGreen.copy(alpha = 0.35f) else OfflineRed.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(10.dp)
            ) {
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .background(OnlineGreen.copy(alpha = 0.35f), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(if (isOnline) OnlineGreen else OfflineRed, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            val label = if (isOnline) "Friend connected" else "Friend offline"

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.3.sp
                ),
                color = if (isOnline) OnlineGreen else OfflineRed
            )
        }
    }
}
