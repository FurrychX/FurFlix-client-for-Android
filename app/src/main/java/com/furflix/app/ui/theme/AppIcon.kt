package com.furflix.app.ui.theme

import androidx.annotation.DrawableRes
import com.furflix.app.R

enum class AppIcon(
    val id: String,
    val titleRes: Int,
    val aliasName: String,
    @DrawableRes val previewRes: Int
) {
    DARK("dark", R.string.theme_dark, "com.furflix.app.MainActivityDefault", R.drawable.preview_icon_dark),
    PINK("pink", R.string.theme_cherry_blossom, "com.furflix.app.MainActivityPink", R.drawable.preview_icon_pink),
    MINT("mint", R.string.theme_mint_fresh, "com.furflix.app.MainActivityMint", R.drawable.preview_icon_mint),
    GOLD("gold", R.string.theme_gold, "com.furflix.app.MainActivityGold", R.drawable.preview_icon_gold),
    PURPLE("purple", R.string.theme_neon_galaxy, "com.furflix.app.MainActivityPurple", R.drawable.preview_icon_purple),
    NETFLIX("netflix", R.string.theme_netflix, "com.furflix.app.MainActivityNetflix", R.drawable.preview_icon_netflix),
    SUMMER_SKY("summer_sky", R.string.theme_summer_sky, "com.furflix.app.MainActivityUkraine", R.drawable.preview_icon_ukraine),
    PRIDE("pride", R.string.theme_pride, "com.furflix.app.MainActivityPride", R.drawable.preview_icon_pride);

    companion object {
        fun fromId(id: String): AppIcon = entries.find { it.id == id } ?: DARK
    }
}
