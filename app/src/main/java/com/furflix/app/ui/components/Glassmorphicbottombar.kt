package com.furflix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * A single tab in the glassmorphic bottom bar. [color] is the tab's accent —
 * it drives the glow and bottom gleam when this tab is selected.
 */
data class GlassBarTab(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Furry-themed icon set for the bottom bar (paw print, tail, ear-notched
 * search, muzzle profile). Stroke-style, 24x24 viewport, matches the
 * panel's original line-icon weight.
 */
object FurryTabIcons {

    /** Browse — paw-outline, stroke-style paw print (Ionicons), 512→24 vp */
    val PawOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "PawOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.White), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21.457f, 7.973f)
                arcTo(1.418f, 1.418f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 20.933f, y1 = 7.875f)
                lineTo(21.121f, 7.875f)
                curveTo(20.175f, 8.016f, 19.115f, 8.775f, 18.559f, 10.114f)
                curveTo(17.892f, 11.716f, 18.199f, 13.355f, 19.250f, 13.777f)
                arcTo(1.416f, 1.416f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 19.772f, y1 = 13.875f)
                curveTo(20.723f, 13.875f, 21.797f, 12.984f, 22.358f, 11.636f)
                curveTo(22.815f, 10.034f, 22.503f, 8.395f, 21.457f, 7.973f)
                close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.White), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15.356f, 14.226f)
                curveTo(14.053f, 12.063f, 13.491f, 11.250f, 12.000f, 11.250f)
                curveTo(12.000f, 11.250f, 9.942f, 12.068f, 8.639f, 14.226f)
                curveTo(7.523f, 16.071f, 5.269f, 16.224f, 4.706f, 17.791f)
                arcTo(2.386f, 2.386f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 4.537f, y1 = 18.694f)
                curveTo(4.537f, 19.968f, 5.512f, 21.000f, 6.712f, 21.000f)
                curveTo(8.203f, 21.000f, 10.233f, 19.810f, 12.005f, 19.810f)
                curveTo(12.005f, 19.810f, 15.797f, 21.000f, 17.288f, 21.000f)
                curveTo(18.488f, 21.000f, 19.458f, 19.969f, 19.458f, 18.694f)
                arcTo(2.391f, 2.391f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 19.284f, y1 = 17.791f)
                curveTo(18.722f, 16.219f, 16.472f, 16.071f, 15.356f, 14.226f)
                close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.White), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9.024f, 9.188f)
                arcTo(1.244f, 1.244f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 9.211f, y1 = 9.328f)
                curveTo(10.299f, 9.170f, 10.979f, 7.663f, 10.732f, 5.960f)
                curveTo(10.500f, 4.200f, 9.526f, 3.000f, 8.507f, 3.000f)
                arcTo(1.244f, 1.244f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 8.320f, y1 = 3.141f)
                curveTo(7.232f, 3.299f, 6.553f, 4.806f, 6.799f, 6.509f)
                curveTo(7.031f, 7.982f, 8.005f, 9.188f, 9.024f, 9.188f)
                close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.White), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(17.199f, 6.382f)
                curveTo(17.446f, 4.680f, 16.767f, 3.172f, 15.679f, 3.014f)
                arcTo(1.244f, 1.244f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 15.491f, y1 = 3.155f)
                curveTo(14.473f, 3.155f, 13.500f, 4.355f, 13.268f, 5.960f)
                curveTo(13.021f, 7.663f, 13.701f, 9.170f, 14.789f, 9.328f)
                arcTo(1.244f, 1.244f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 14.976f, y1 = 9.469f)
                curveTo(15.995f, 9.188f, 16.969f, 7.982f, 17.199f, 6.382f)
                close()
            }
            path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.White), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4.958f, 13.777f)
                curveTo(6.008f, 13.355f, 6.314f, 11.714f, 5.648f, 10.114f)
                curveTo(5.087f, 8.766f, 4.013f, 7.875f, 3.065f, 7.875f)
                arcTo(1.416f, 1.416f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 2.542f, y1 = 7.973f)
                curveTo(1.492f, 8.395f, 1.186f, 10.036f, 1.852f, 11.636f)
                curveTo(2.413f, 12.984f, 3.487f, 13.875f, 4.435f, 13.875f)
                arcTo(1.416f, 1.416f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 4.958f, y1 = 13.777f)
                close()
            }
        }.build()
    }

    /** Latest — curled tail with a spark/star, signals "fresh / new" */
    val TailSpark: ImageVector by lazy {
        ImageVector.Builder(
            name = "TailSpark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 19f)
                curveTo(3.2f, 15.5f, 4.5f, 11.5f, 7.5f, 9.3f)
                curveTo(10.5f, 7.1f, 13f, 8.2f, 13.6f, 10.3f)
                curveTo(14.1f, 12f, 13f, 13.4f, 11.5f, 13.1f)
                curveTo(10.3f, 12.9f, 9.8f, 11.7f, 10.4f, 10.8f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(18f, 3.5f)
                lineTo(18.9f, 6.1f)
                lineTo(21.5f, 7f)
                lineTo(18.9f, 7.9f)
                lineTo(18f, 10.5f)
                lineTo(17.1f, 7.9f)
                lineTo(14.5f, 7f)
                lineTo(17.1f, 6.1f)
                close()
            }
        }.build()
    }

    /** Search — magnifying glass whose handle sweeps into a long, elegant dinosaur tail with dorsal spikes */
    val SharkSearch: ImageVector by lazy {
        ImageVector.Builder(
            name = "DinoTailSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Magnifying-glass ring
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.8f, 9.6f)
                arcTo(
                    6.2f, 6.2f, 0f,
                    isMoreThanHalf = true, isPositiveArc = false,
                    x1 = 3.4f, y1 = 9.6f
                )
                arcTo(
                    6.2f, 6.2f, 0f,
                    isMoreThanHalf = true, isPositiveArc = false,
                    x1 = 15.8f, y1 = 9.6f
                )
            }
            // Dinosaur tail: filled, tapering S-curve with dorsal spikes
            path(fill = SolidColor(Color.White)) {
                moveTo(12.45f, 13.74f)
                lineTo(12.66f, 12.89f)
                lineTo(12.90f, 12.08f)
                lineTo(13.17f, 11.30f)
                lineTo(13.46f, 10.55f)
                lineTo(13.78f, 9.84f)
                lineTo(14.12f, 9.16f)
                lineTo(14.48f, 8.52f)
                lineTo(14.86f, 7.91f)
                lineTo(15.26f, 7.34f)
                lineTo(15.67f, 6.80f)
                lineTo(16.09f, 6.29f)
                lineTo(16.53f, 5.82f)
                lineTo(16.98f, 5.38f)
                lineTo(17.43f, 4.98f)
                lineTo(17.89f, 4.61f)
                lineTo(18.35f, 4.27f)
                lineTo(18.82f, 3.97f)
                lineTo(19.29f, 3.70f)
                lineTo(19.75f, 3.46f)
                lineTo(20.22f, 3.26f)
                lineTo(20.67f, 3.09f)
                lineTo(21.12f, 2.96f)
                lineTo(21.57f, 2.86f)
                lineTo(22.00f, 2.80f)
                lineTo(21.63f, 3.02f)
                lineTo(21.25f, 3.25f)
                lineTo(20.88f, 3.51f)
                lineTo(20.52f, 3.79f)
                lineTo(20.15f, 4.09f)
                lineTo(19.79f, 4.42f)
                lineTo(19.44f, 4.76f)
                lineTo(19.10f, 5.14f)
                lineTo(18.76f, 5.53f)
                lineTo(18.44f, 5.95f)
                lineTo(18.12f, 6.39f)
                lineTo(17.82f, 6.85f)
                lineTo(17.53f, 7.34f)
                lineTo(17.25f, 7.85f)
                lineTo(16.99f, 8.38f)
                lineTo(16.74f, 8.94f)
                lineTo(16.52f, 9.52f)
                lineTo(16.31f, 10.12f)
                lineTo(16.12f, 10.75f)
                lineTo(15.95f, 11.39f)
                lineTo(15.81f, 12.07f)
                lineTo(15.68f, 12.76f)
                lineTo(15.59f, 13.49f)
                lineTo(15.51f, 14.23f)
                close()
            }
            // Dorsal spikes
            path(fill = SolidColor(Color.White)) {
                // Spike 1
                moveTo(14.70f, 10.94f)
                lineTo(13.67f, 10.85f)
                lineTo(15.10f, 9.91f)
                close()
                // Spike 2
                moveTo(16.02f, 8.00f)
                lineTo(14.86f, 7.20f)
                lineTo(16.62f, 7.07f)
                close()
                // Spike 3
                moveTo(17.70f, 5.69f)
                lineTo(17.17f, 4.38f)
                lineTo(18.48f, 4.91f)
                close()
                // Spike 4
                moveTo(19.57f, 4.01f)
                lineTo(19.86f, 2.84f)
                lineTo(20.50f, 3.44f)
                close()
            }
        }.build()
    }

    /** Profile — rabbit-variant-outline (MDI), filled silhouette with cutouts */
    val MuzzleProfile: ImageVector by lazy {
        ImageVector.Builder(
            name = "RabbitVariantOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Main body
                moveTo(17f, 14f)
                curveTo(16.76f, 13.76f, 16.56f, 13.5f, 16.35f, 13.25f)
                curveTo(17.5f, 11.5f, 19f, 8.56f, 19f, 5f)
                curveTo(19f, 3.05f, 18.26f, 2f, 17f, 2f)
                curveTo(15.46f, 2f, 13.04f, 4.06f, 12f, 7.97f)
                curveTo(10.96f, 4.06f, 8.54f, 2f, 7f, 2f)
                curveTo(5.74f, 2f, 5f, 3.05f, 5f, 5f)
                curveTo(5f, 8.56f, 6.5f, 11.5f, 7.65f, 13.25f)
                curveTo(7.44f, 13.5f, 7.24f, 13.76f, 7f, 14f)
                curveTo(6.75f, 14.25f, 5f, 15.39f, 5f, 17.5f)
                curveTo(5f, 20f, 7f, 22f, 9.5f, 22f)
                curveTo(11f, 22f, 12f, 21.5f, 12f, 21.5f)
                curveTo(12f, 21.5f, 13f, 22f, 14.5f, 22f)
                curveTo(17f, 22f, 19f, 20f, 19f, 17.5f)
                curveTo(19f, 15.39f, 17.25f, 14.25f, 17f, 14f)
                close()
                // Left inner ear
                moveTo(16.88f, 4.03f)
                curveTo(16.94f, 4.2f, 17f, 4.5f, 17f, 5f)
                curveTo(17f, 7.84f, 15.89f, 10.24f, 14.93f, 11.78f)
                curveTo(14.55f, 11.5f, 14.1f, 11.3f, 13.53f, 11.16f)
                curveTo(13.77f, 6.64f, 15.97f, 4.33f, 16.88f, 4.03f)
                close()
                // Right inner ear
                moveTo(7f, 5f)
                curveTo(7f, 4.5f, 7.06f, 4.2f, 7.12f, 4.03f)
                curveTo(8.03f, 4.33f, 10.23f, 6.64f, 10.5f, 11.16f)
                curveTo(9.9f, 11.3f, 9.45f, 11.5f, 9.08f, 11.78f)
                curveTo(8.11f, 10.24f, 7f, 7.84f, 7f, 5f)
                close()
                // Face / chest detail
                moveTo(14.5f, 20f)
                curveTo(13.5f, 20f, 12.7f, 19.67f, 12.28f, 19.44f)
                curveTo(12.7f, 19.26f, 13f, 18.73f, 13f, 18.5f)
                curveTo(13f, 18.22f, 12.55f, 18f, 12f, 18f)
                curveTo(11.45f, 18f, 11f, 18.22f, 11f, 18.5f)
                curveTo(11f, 18.73f, 11.3f, 19.26f, 11.72f, 19.44f)
                curveTo(11.3f, 19.67f, 10.5f, 20f, 9.5f, 20f)
                curveTo(8.12f, 20f, 7f, 18.88f, 7f, 17.5f)
                curveTo(7f, 16.8f, 7.43f, 16.26f, 8f, 15.77f)
                curveTo(8.44f, 15.41f, 8.61f, 15.25f, 9.3f, 14.4f)
                curveTo(10.06f, 13.45f, 10.39f, 13f, 12f, 13f)
                curveTo(13.61f, 13f, 13.94f, 13.45f, 14.7f, 14.4f)
                curveTo(15.39f, 15.25f, 15.56f, 15.41f, 16f, 15.77f)
                curveTo(16.57f, 16.26f, 17f, 16.8f, 17f, 17.5f)
                curveTo(17f, 18.88f, 15.88f, 20f, 14.5f, 20f)
                close()
                // Right eye
                moveTo(14f, 16f)
                curveTo(14f, 16.41f, 13.78f, 16.75f, 13.5f, 16.75f)
                curveTo(13.22f, 16.75f, 13f, 16.41f, 13f, 16f)
                curveTo(13f, 15.59f, 13.22f, 15.25f, 13.5f, 15.25f)
                curveTo(13.78f, 15.25f, 14f, 15.59f, 14f, 16f)
                close()
                // Left eye
                moveTo(11f, 16f)
                curveTo(11f, 16.41f, 10.78f, 16.75f, 10.5f, 16.75f)
                curveTo(10.22f, 16.75f, 10f, 16.41f, 10f, 16f)
                curveTo(10f, 15.59f, 10.22f, 15.25f, 10.5f, 15.25f)
                curveTo(10.78f, 15.25f, 11f, 15.59f, 11f, 16f)
                close()
            }
        }.build()
    }
}

/**
 * The bar itself — frosted glass pill with a color glow behind the selected
 * tab and a matching gleam along the bottom edge. Can also be used directly
 * (outside [GlassBottomNavScaffold]) if you already manage your own
 * [HazeState] / [hazeSource].
 */
@Composable
fun GlassmorphicBottomBar(
    tabs: List<GlassBarTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    blurRadius: Dp = 30.dp
) {
    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy,
        ),
        label = "animatedSelectedIndex"
    )
    val animatedColor by animateColorAsState(
        targetValue = if (selectedIndex >= 0) tabs[selectedIndex].color else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animatedColor"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 0) Drop shadow cast by the bar itself onto whatever is behind —
        // this is what separates the bar from plain dark backgrounds where
        // there's nothing for the blur to "grab onto". Independent of haze.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.35f))
                .blur(radius = 16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        )

        // 1) Frosted glass base — samples & blurs whatever is behind (the
        // scrolling content registered via hazeSource), then a solid dark
        // scrim ON TOP of the blur result. The scrim alpha is high enough
        // that the bar reads as a clearly defined surface (not a smear)
        // whether it's sitting over a busy image or a flat dark screen.
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
                .background(Color(0xFF0B0C0E).copy(alpha = 0.55f))
        )

        // 2) Soft color glow behind the currently selected tab. Clipped to
        // the bar's own bounds (not Unbounded) so it never bleeds upward
        // into content sitting just above the bar — that bleed was what
        // smeared into text like tag chips in the screenshot.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .blur(radius = 24.dp)
        ) {
            val tabWidth = size.width / tabs.size
            drawCircle(
                color = animatedColor.copy(alpha = 0.5f),
                radius = size.height / 1.6f,
                center = Offset(
                    x = (tabWidth * animatedSelectedIndex) + tabWidth / 2,
                    y = size.height / 2
                )
            )
        }

        // 3) Convex effect: A subtle vertical gradient border to create a 3D bevel
        // look on the edges without affecting the middle frosted glass.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(
                    width = 1.dp, // slightly thicker than hairline for the convex effect
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f), // Top highlight
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f) // Bottom shadow
                        )
                    ),
                    shape = shape
                )
        )

        // 4) Bottom gleam — a short segment of the border, positioned under
        // the selected tab, tinted with its accent color.
        Canvas(modifier = Modifier.fillMaxSize().clip(shape)) {
            val path = Path().apply {
                addRoundRect(RoundRect(size.toRect(), CornerRadius(20.dp.toPx())))
            }
            val length = PathMeasure().apply { setPath(path, false) }.length
            val tabWidth = size.width / tabs.size

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0f),
                        animatedColor.copy(alpha = 1f),
                        animatedColor.copy(alpha = 1f),
                        animatedColor.copy(alpha = 0f),
                    ),
                    startX = tabWidth * animatedSelectedIndex,
                    endX = tabWidth * (animatedSelectedIndex + 1),
                ),
                style = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(length / 2, length)
                    )
                )
            )
        }

        // 5) Tab row on top of everything.
        GlassBarTabsRow(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun GlassBarTabsRow(
    tabs: List<GlassBarTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        ),
        LocalContentColor provides Color.White
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedIndex == index
                val noSelection = selectedIndex < 0

                val alpha by animateFloatAsState(
                    targetValue = if (noSelection) 0.8f else if (selected) 1f else 0.35f,
                    label = "tabAlpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (noSelection) 1f else if (selected) 1f else 0.96f,
                    visibilityThreshold = 0.000001f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                    ),
                    label = "tabScale"
                )
                val liftScale by animateFloatAsState(
                    targetValue = if (selected && !noSelection) 1.08f else 1f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                    ),
                    label = "liftScale"
                )

                Column(
                    modifier = Modifier
                        .scale(scale)
                        .alpha(alpha)
                        .fillMaxHeight()
                        .weight(1f)
                        .pointerInput(tab) {
                            detectTapGestures {
                                onTabSelected(index)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        modifier = Modifier.scale(liftScale)
                    )
                    Text(text = tab.title)
                }
            }
        }
    }
}