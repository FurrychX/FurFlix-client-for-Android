package com.furflix.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A single action button in [GlassActionBar]. There is no "selected" state
 * — each button fires an action (back, share, favorite, download, etc.).
 * [color] tints the icon only while [isActive] is true (e.g. the Favorite
 * icon once the item has been marked as a favorite); otherwise icons share a
 * neutral color so the bar stays calm and legible.
 */
data class GlassActionItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val isActive: Boolean = false
)

/**
 * Glass toolbar for standalone actions (Back / Share / Favorite / Download,
 * etc.). Redesigned as a flat, stable rectangular card — no glow pills, no
 * per-icon colored halos (those tended to look like they were "spilling"
 * past the bar's edges). Buttons are separated by simple hairline dividers,
 * the classic toolbar pattern, so the whole thing reads as one calm surface
 * instead of several competing shapes.
 */
@Composable
fun GlassActionBar(
    items: List<GlassActionItem>,
    onItemClick: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    blurRadius: Dp = 30.dp
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Drop shadow — keeps the card legible over plain/dark backgrounds,
        // independent of what haze finds to blur behind it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.35f))
                .blur(radius = 16.dp)
        )

        // Frosted glass + solid dark scrim on top, so the bar reads as a
        // clearly defined surface rather than a smear over content/text.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .hazeEffect(state = hazeState) {
                    this.blurRadius = blurRadius
                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.4f)))
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Color(0xFF121316).copy(alpha = 0.68f))
        )

        // Single hairline border all around — no gradient bevel, no top
        // gleam. Flat, even, quiet. This is what keeps the card reading as
        // one solid object instead of a stack of layered effects.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = shape
                )
        )

        ActionRow(items = items, onItemClick = onItemClick)
    }
}

@Composable
private fun ActionRow(
    items: List<GlassActionItem>,
    onItemClick: (Int) -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        ),
        LocalContentColor provides Color.White.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                ActionButton(
                    item = item,
                    onClick = { onItemClick(index) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                // Hairline divider between buttons — not after the last one.
                if (index != items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.4f)
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    item: GlassActionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Bumped on every tap; drives a quick, contained bounce on the icon
    // only — no halo, just a tactile nudge.
    var tapTick by remember { mutableIntStateOf(0) }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (tapTick > 0) {
            androidx.compose.animation.core.keyframes {
                durationMillis = 440
                0.86f at 60
                1.10f at 220
                1f at 440
            }
        } else {
            spring(stiffness = Spring.StiffnessMedium)
        },
        label = "actionButtonScale"
    )

    // Neutral by default; colored when the item is in an active state (e.g.
    // Favorite once marked). This is the resting-state accent.
    val restingColor = if (item.isActive) item.color else Color.White.copy(alpha = 0.92f)

    // Fast color-fill flash on tap: `isFlashing` snaps the target color to
    // the item's accent immediately, then a coroutine flips it back after a
    // short hold so animateColorAsState eases it back to restingColor.
    var isFlashing by remember { mutableStateOf(false) }
    val iconColor by animateColorAsState(
        targetValue = if (isFlashing) item.color else restingColor,
        animationSpec = if (isFlashing) {
            snap()
        } else {
            tween(durationMillis = 760)
        },
        label = "actionIconColor"
    )

    // Beautiful UI/UX burst animation for active state
    var wasActive by remember { mutableStateOf(item.isActive) }
    var burstTrigger by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(item.isActive) {
        if (item.isActive && !wasActive) {
            burstTrigger = true
            delay(1200)
            burstTrigger = false
        }
        wasActive = item.isActive
    }

    val burstProgress by animateFloatAsState(
        targetValue = if (burstTrigger) 1f else 0f,
        animationSpec = if (burstTrigger) tween(1200, easing = FastOutSlowInEasing) else snap(),
        label = "burstProgress"
    )

    val sparks = remember(burstTrigger) {
        if (burstTrigger) {
            List(12) { i ->
                val angle = (i * 30 + Random.nextInt(-10, 10)) * (Math.PI / 180f).toFloat()
                val speed = Random.nextFloat() * 40f + 40f
                val length = Random.nextFloat() * 12f + 8f
                Triple(angle, speed, length)
            }
        } else emptyList()
    }

    Column(
        modifier = modifier
            .pointerInput(item) {
                detectTapGestures {
                    tapTick++
                    onClick()
                    scope.launch {
                        isFlashing = true
                        delay(180)
                        isFlashing = false
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (burstProgress > 0f && burstProgress < 1f) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val ringAlpha = 1f - burstProgress
                    val p = 1f - (1f - burstProgress) * (1f - burstProgress)

                    // Shockwave ring
                    drawCircle(
                        color = item.color.copy(alpha = ringAlpha * 0.6f),
                        radius = p * 30.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx() * (1f - burstProgress))
                    )

                    // Sparks
                    sparks.forEach { (angle, speed, sparkLen) ->
                        val dist = speed * p.dp.toPx()
                        val currentLen = sparkLen.dp.toPx() * (1f - burstProgress)
                        
                        val startX = center.x + cos(angle) * dist
                        val startY = center.y + sin(angle) * dist
                        val endX = center.x + cos(angle) * (dist + currentLen)
                        val endY = center.y + sin(angle) * (dist + currentLen)

                        drawLine(
                            color = item.color.copy(alpha = ringAlpha),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx() * (1f - burstProgress),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            AnimatedContent(
                targetState = item.icon,
                transitionSpec = {
                    (scaleIn(tween(500)) + fadeIn(tween(500))) togetherWith (scaleOut(tween(500)) + fadeOut(tween(500)))
                },
                label = "iconSwap"
            ) { targetIcon ->
                Icon(
                    imageVector = targetIcon,
                    contentDescription = item.title,
                    tint = iconColor,
                    modifier = Modifier.scale(scale)
                )
            }
        }
        Text(text = item.title, color = Color.White.copy(alpha = 0.75f))
    }
}