package com.furflix.app.ui.theme

import androidx.compose.ui.graphics.Color

import com.furflix.app.R

enum class ThemePreference(val id: Int, val titleRes: Int, val color: Color) {
    DEFAULT(0, R.string.theme_default, Color(0xFFE0E0E0)),
    CHERRY_BLOSSOM(5, R.string.theme_cherry_blossom, Color(0xFFFFB7C5)),
    MINT_FRESH(6, R.string.theme_mint_fresh, Color(0xFF00FA9A)),
    GOLD(4, R.string.theme_gold, Color(0xFFFFD700)),
    PRIDE(3, R.string.theme_pride, Color(0xFFFF007F)),
    NEON_GALAXY(7, R.string.theme_neon_galaxy, Color(0xFFB026FF)),
    SUMMER_SKY(1, R.string.theme_summer_sky, Color(0xFF005BBC)),
    DARK(2, R.string.theme_dark, Color(0xFF808080));

    companion object {
        fun fromId(id: Int): ThemePreference {
            return entries.find { it.id == id } ?: DEFAULT
        }
    }
}
