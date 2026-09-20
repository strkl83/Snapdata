package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SnapArchiveEntity
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.AccentWarning
import com.example.ui.theme.SnapCyan
import com.example.ui.theme.SnapPurple
import com.example.ui.theme.SnapYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CategoryBadge(mediaType: String, modifier: Modifier = Modifier) {
    val (color, label, icon) = when (mediaType) {
        "PHOTO" -> Triple(SnapYellow, "Photo", Icons.Default.Image)
        "VIDEO" -> Triple(SnapCyan, "Video", Icons.Default.Movie)
        "OVERLAY" -> Triple(SnapPurple, "Overlay", Icons.Default.Image)
        "TEXT_CHAT" -> Triple(Color(0xFF8E8E93), "Chat", Icons.Default.ChatBubble)
        "SNAP_RECEIVED" -> Triple(Color(0xFFFF2A6D), "Snap Recv", Icons.Default.ChatBubble)
        "SNAP_SENT" -> Triple(Color(0xFF00E676), "Snap Sent", Icons.Default.ChatBubble)
        else -> Triple(SnapYellow, mediaType, Icons.Default.Image)
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusBadge(isBroken: Boolean, modifier: Modifier = Modifier) {
    val color = if (isBroken) AccentError else AccentSuccess
    val label = if (isBroken) "Broken Link" else "Link Fixed"
    val icon = if (isBroken) Icons.Default.Warning else Icons.Default.CheckCircle

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SenderAvatar(sender: String, modifier: Modifier = Modifier) {
    val initial = sender.take(1).uppercase(Locale.ROOT)
    val bgColor = when (sender) {
        "Sarah Miller" -> Color(0xFFFF2A6D)
        "Alex Chen" -> Color(0xFF00E5FF)
        "Maya Lin" -> Color(0xFFD500F9)
        "Memories" -> SnapYellow
        "Me" -> Color(0xFF00E676)
        else -> Color(0xFFFFAB00)
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.25f))
            .border(1.5.dp, bgColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = bgColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
