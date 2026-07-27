package com.furflix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.furflix.app.data.model.Submission
import com.furflix.app.ui.theme.*

@Composable
fun SubmissionCard(
    submission: Submission,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    compact: Boolean = false,
    onAuthorClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (compact) 8.dp else 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardDark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            val cardShape = if (!showTitle || compact)
                RoundedCornerShape(if (compact) 8.dp else 12.dp)
            else
                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            var imageLoaded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (compact) 1f else 4f / 3f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(submission.thumbnailUrl)
                        .crossfade(300)
                        .build(),
                    imageLoader = ImageLoaderFactory.getInstance(LocalContext.current),
                    contentDescription = submission.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(DarkSurfaceVariant),
                    error = ColorPainter(DarkSurfaceVariant),
                    onLoading = { imageLoaded = false },
                    onSuccess = { imageLoaded = true },
                    onError = { imageLoaded = true }
                )

                if (!imageLoaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(cardShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = FA_Accent.copy(alpha = 0.5f),
                            strokeWidth = 2.dp
                        )
                    }
                }

                // Soft bottom scrim — fades in only over the lower third so
                // the artwork stays clean; just enough to anchor the author text.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    0.80f to Color.Black.copy(alpha = if (compact) 0.16f else 0.20f),
                                    1.0f to Color.Black.copy(alpha = if (compact) 0.40f else 0.48f)
                                )
                            )
                        )
                )

                // Rating badge
                if (submission.rating.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .padding(if (compact) 4.dp else 8.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(if (compact) 4.dp else 6.dp),
                        color = when {
                            submission.rating.contains("General", true) -> Rating_General
                            submission.rating.contains("Mature", true)  -> Rating_Mature
                            submission.rating.contains("Adult", true)   -> Rating_Adult
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Text(
                            text = submission.rating.take(1),
                            modifier = Modifier.padding(horizontal = if (compact) 4.dp else 8.dp, vertical = 2.dp),
                            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                // Author name (always shown inside image) — tappable, opens the artist profile
                if (submission.author.isNotEmpty()) {
                    val baseStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
                    Text(
                        text = if (compact) submission.author else "by ${submission.author}",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .then(
                                if (onAuthorClick != null) Modifier.clickable {
                                    onAuthorClick(submission.authorUsername.ifEmpty { submission.author })
                                } else Modifier
                            )
                            .padding(if (compact) 6.dp else 10.dp),
                        style = baseStyle.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = Offset(0f, 1f),
                                blurRadius = 4f
                            )
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Title inside image when showTitle=true and compact
                if (showTitle && compact) {
                    // Title is shown inside the card below, not overlaid when compact
                }
            }

            // Bottom section — only shown in full (1-column) mode or when showTitle=true and not compact
            if (!compact) {
                if (showTitle) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = submission.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (submission.favorites > 0) {
                                    StatChip(icon = Icons.Default.Favorite, text = formatCount(submission.favorites), color = FA_Accent)
                                }
                                if (submission.comments > 0) {
                                    StatChip(icon = Icons.Default.ChatBubble, text = formatCount(submission.comments), color = FA_LightBlue)
                                }
                                if (submission.views > 0) {
                                    StatChip(icon = Icons.Default.Visibility, text = formatCount(submission.views), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    // No title, no padding — image fills card edge-to-edge
                }
            } else {
                // Compact mode: show title below image as small text if showTitle=true
                if (showTitle) {
                    Text(
                        text = submission.title,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(width = 14.dp, height = 14.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
