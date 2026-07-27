package com.furflix.app.ui.components

import androidx.compose.ui.graphics.Color
import com.furflix.app.ui.theme.Dimens
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

val DefaultHazeStyle = HazeStyle(
    tint = HazeTint(Color.Black.copy(alpha = Dimens.hazeTintAlpha)),
    blurRadius = Dimens.hazeBlurRadius
)
