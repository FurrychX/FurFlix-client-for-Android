package com.furflix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.furflix.app.ui.theme.ChipBackground
import com.furflix.app.ui.theme.ChipBorder
import com.furflix.app.ui.theme.FA_Accent
import com.furflix.app.ui.theme.SubtleGray

/**
 * Pill-style segmented tab selector — the shared replacement for the default
 * Material TabRow across detail/profile screens.
 */
@Composable
fun CustomTabRow(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChipBackground)
            .border(1.dp, ChipBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedTab == index
            val bgColor by animateColorAsState(
                targetValue = if (selected) FA_Accent.copy(alpha = 0.12f) else Color.Transparent,
                animationSpec = tween(250),
                label = "tabBg$index"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) FA_Accent else SubtleGray,
                animationSpec = tween(250),
                label = "tabText$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
