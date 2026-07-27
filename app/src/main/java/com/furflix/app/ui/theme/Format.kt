package com.furflix.app.ui.theme

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(java.util.Locale.US, "%.1fk", count / 1_000.0)
    else -> count.toString()
}
