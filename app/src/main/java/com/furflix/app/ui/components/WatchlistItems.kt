package com.furflix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.furflix.app.data.model.WatchlistUser
import com.furflix.app.ui.theme.*

@Composable
fun WatchlistItem(user: WatchlistUser, onClick: () -> Unit) {
    val avatarUrl = if (user.avatarUrl.isNotEmpty())
        user.avatarUrl
    else
        "https://a.furaffinity.net/${user.username}.gif"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(start = 0.dp, end = 14.dp, top = 11.dp, bottom = 11.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(FA_Accent)
        )

        Spacer(Modifier.width(12.dp))

        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar of ${user.displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(1.5.dp, FA_Accent.copy(alpha = 0.6f), CircleShape)
                .background(DarkSurfaceVariant)
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = user.displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}

@Composable
fun WatchlistGridItem(user: WatchlistUser, onClick: () -> Unit) {
    val avatarUrl = if (user.avatarUrl.isNotEmpty())
        user.avatarUrl
    else
        "https://a.furaffinity.net/${user.username}.gif"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar of ${user.displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(1.5.dp, FA_Accent.copy(alpha = 0.6f), CircleShape)
                .background(DarkSurfaceVariant)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = user.displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
