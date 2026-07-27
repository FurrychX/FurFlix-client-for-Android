package com.furflix.app.data.model

data class ContactLink(
    val type: String,   // website, twitter, telegram, youtube...
    val label: String,  // display text (usually the URL or handle)
    val url: String
)

data class UserProfile(
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val isWatching: Boolean,
    val watchUrl: String,
    val profileText: String = "",
    val galleryCount: Int = 0,
    val favoritesCount: Int = 0,
    val watchersCount: Int = 0,
    val watchingCount: Int = 0,
    val commentsCount: Int = 0,
    // Extended info parsed from the user page
    val userTitle: String = "",          // e.g. "Fursuiter", "Watcher", "Artist"
    val registeredText: String = "",     // e.g. "19 years ago"
    val noteUrl: String = "",            // /newpm/{user}/ link
    val views: Int = 0,
    val journalsCount: Int = 0,
    val acceptingTrades: String = "",    // "Yes" / "No" / "" (unknown)
    val acceptingCommissions: String = "",
    val species: String = "",
    val contactLinks: List<ContactLink> = emptyList()
)
