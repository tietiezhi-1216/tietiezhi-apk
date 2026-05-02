package com.tietiezhi.apk.ui.chat.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tietiezhi.apk.ui.theme.AssistantBubble

@Composable
fun StreamingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
                .background(AssistantBubble).padding(12.dp)
        ) {
            val transition = rememberInfiniteTransition()
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.2f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(modifier = Modifier.size(8.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), CircleShape))
                }
            }
        }
    }
}
