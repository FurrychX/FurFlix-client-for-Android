package com.furflix.app.ui.screens

import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.furflix.app.ui.components.Particle
import com.furflix.app.ui.theme.Cyan
import com.furflix.app.ui.theme.Pink
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val pawScaleAnim = remember { Animatable(0f) }
    val pawBounceY = remember { Animatable(0f) }
    var glowAlpha by remember { mutableFloatStateOf(0f) }
    var titleAlpha by remember { mutableFloatStateOf(0f) }

    var showLandingBurst by remember { mutableStateOf(false) }
    val landingBurstProgress = remember { Animatable(0f) }

    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val landingParticles = remember(primaryColor) {
        val colors = listOf(primaryColor, Cyan, Pink, Color.White)
        List(36) { i ->
            val angle = (i * (360f / 36) + Random.nextInt(-5, 5)).toFloat() * (Math.PI / 180f).toFloat()
            Particle(
                id = i,
                angle = angle,
                distance = Random.nextFloat() * 110f + 70f,
                size = Random.nextFloat() * 5f + 3f,
                color = colors[Random.nextInt(colors.size)]
            )
        }
    }

    LaunchedEffect(Unit) {
        pawScaleAnim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 350f))
        glowAlpha = 0.6f
        delay(250)
        titleAlpha = 1f
        delay(150)
        // Quick hop up
        pawBounceY.animateTo(-16f, tween(135, easing = FastOutSlowInEasing))
        // Come down — burst fires just as paw hits bottom
        launch {
            delay(20)
            showLandingBurst = true
            landingBurstProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        pawBounceY.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        delay(660)
        onSplashFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (showLandingBurst && landingBurstProgress.value < 1f) {
            val progress = landingBurstProgress.value
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f - with(density) { 18.dp.toPx() }
                val alpha = if (progress > 0.5f) (1f - progress) * 2f else 1f

                landingParticles.forEach { p ->
                    val currentDist = p.distance * (1f - (1f - progress) * (1f - progress))
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size * (1f - progress * 0.3f),
                        center = Offset(
                            x = cx + cos(p.angle) * currentDist,
                            y = cy + sin(p.angle) * currentDist
                        ),
                        style = Fill
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { translationY = pawBounceY.value },
                contentAlignment = Alignment.Center
            ) {
                GlowCircle(
                    modifier = Modifier.size(80.dp).alpha(glowAlpha)
                )
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp).scale(pawScaleAnim.value)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "FurFlix",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(titleAlpha)
            )
        }
    }
}

@Composable
private fun GlowCircle(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.4f),
                    primaryColor.copy(alpha = 0.1f),
                    Color.Transparent
                )
            ),
            radius = radius
        )
    }
}
