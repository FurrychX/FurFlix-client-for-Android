package com.furflix.app.data.repository

import android.content.Context
import android.annotation.SuppressLint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.furflix.app.data.model.Submission
import com.furflix.app.data.remote.FurAffinityScraper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.furflix.app.data.local.TagsDatabase

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "furflix_prefs")

@SuppressLint("StaticFieldLeak")
class FurRepository(private val context: Context) {

    companion object {
        private val COOKIES_KEY = stringPreferencesKey("fa_cookies")
        private val USERNAME_KEY = stringPreferencesKey("fa_username")
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        private val IS_GRID_KEY = booleanPreferencesKey("is_grid")
        private val DEFAULT_TAB_KEY = androidx.datastore.preferences.core.intPreferencesKey("default_tab")
        private val THEME_ID_KEY = androidx.datastore.preferences.core.intPreferencesKey("theme_id")
        private val APP_ICON_KEY = stringPreferencesKey("app_icon")
        private val MUTED_WORDS_KEY = stringPreferencesKey("muted_words")
        private val BLOCKED_ARTISTS_KEY = stringPreferencesKey("blocked_artists")
        private val NSFW_ENABLED_KEY = booleanPreferencesKey("nsfw_enabled")
    private val DOWNLOAD_FOLDER_KEY = stringPreferencesKey("download_folder")

        @Volatile
        private var INSTANCE: FurRepository? = null

        fun getInstance(context: Context): FurRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FurRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun saveCookiesFromWebView(cookies: String, username: String = "") {
        context.dataStore.edit { prefs ->
            prefs[COOKIES_KEY] = cookies
            if (username.isNotEmpty()) {
                prefs[USERNAME_KEY] = username
            }
        }
        if (username.isNotEmpty()) {
            FurAffinityScraper.setUsername(username)
        }
    }

    suspend fun loadSavedCookies(): Boolean {
        val prefs = context.dataStore.data.first()
        val cookies = prefs[COOKIES_KEY] ?: ""
        if (cookies.isNotEmpty()) {
            FurAffinityScraper.setCookiesFromWebView(cookies)
            val savedUsername = prefs[USERNAME_KEY] ?: ""
            // Guard against a previously-corrupted cached username (e.g. "Logged in",
            // which was a scraping bug that grabbed menu text instead of the real name).
            val isValidUsername = savedUsername.isNotEmpty() &&
                    !savedUsername.contains(" ") &&
                    savedUsername.length < 40
            if (isValidUsername) {
                FurAffinityScraper.setUsername(savedUsername)
            } else {
                if (savedUsername.isNotEmpty()) {
                    // Drop the bad cached value so it doesn't keep overriding a fresh,
                    // correctly-parsed username on future logins.
                    context.dataStore.edit { it.remove(USERNAME_KEY) }
                }
                // Re-derive the real username from the site right away instead of leaving
                // it blank until some other screen happens to call verifyLoginAndGetUsername().
                val freshUsername = FurAffinityScraper.verifyLoginAndGetUsername()
                if (!freshUsername.isNullOrEmpty()) {
                    context.dataStore.edit { it[USERNAME_KEY] = freshUsername }
                }
            }
            return true
        }
        return false
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(COOKIES_KEY)
            prefs.remove(USERNAME_KEY)
        }
        FurAffinityScraper.clearSession()
    }

    fun isLoggedIn(): Boolean = FurAffinityScraper.isLoggedIn()



    suspend fun getUserProfile(username: String) = FurAffinityScraper.getUserProfile(username)

    suspend fun toggleWatch(watchUrl: String) = FurAffinityScraper.toggleWatch(watchUrl)

    suspend fun searchTags(prefix: String, limit: Int = 10): List<String> {
        if (prefix.isBlank()) return emptyList()
        val db = TagsDatabase.getInstance(context)
        return try {
            db.searchTags(prefix, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFavorite(favUrl: String) =
        FurAffinityScraper.toggleFavorite(favUrl)

    suspend fun getWatchlistBy(username: String, page: Int) = FurAffinityScraper.getWatchlistBy(username, page)

    suspend fun getWatchlistTo(username: String, page: Int) = FurAffinityScraper.getWatchlistTo(username, page)

    fun getUsername(): String = FurAffinityScraper.getUsername()

    private suspend fun filterSubmissions(submissions: List<Submission>): List<Submission> {
        val prefs = context.dataStore.data.first()
        val mutedWordsRaw = prefs[MUTED_WORDS_KEY] ?: ""
        val nsfwEnabled = prefs[NSFW_ENABLED_KEY] ?: true

        val mutedWords = if (mutedWordsRaw.isNotEmpty()) mutedWordsRaw.split("|||").map { it.lowercase() } else emptyList()

        return submissions.filter { sub ->
            val titleLower = sub.title.lowercase()
            val rating = sub.rating

            val isMutedWord = mutedWords.any { titleLower.contains(it) }
            val isNsfwBlocked = !nsfwEnabled && (rating == "Adult" || rating == "Mature")

            !isMutedWord && !isNsfwBlocked
        }
    }

    suspend fun getBrowse(page: Int = 1, filters: com.furflix.app.data.model.SearchFilters = com.furflix.app.data.model.SearchFilters()): List<Submission> {
        return filterSubmissions(FurAffinityScraper.getBrowsePage(page, filters))
    }

    suspend fun getLatest(page: Int = 1): List<Submission> {
        return filterSubmissions(FurAffinityScraper.getLatestPage(page))
    }



    suspend fun getSubmission(id: String): Submission? {
        return FurAffinityScraper.getSubmissionDetails(id)
    }

    suspend fun getWatchlist(page: Int = 1): List<Submission> {
        return filterSubmissions(FurAffinityScraper.getWatchlistPage(page))
    }

    suspend fun search(query: String, page: Int, filters: com.furflix.app.data.model.SearchFilters = com.furflix.app.data.model.SearchFilters()): List<Submission> {
        return filterSubmissions(FurAffinityScraper.search(query, page, filters))
    }

    suspend fun getUserGallery(username: String, page: Int): List<Submission> {
        val nsfwEnabled = nsfwEnabledFlow.first()
        return filterSubmissions(FurAffinityScraper.getUserGallery(username, page, nsfwEnabled))
    }

    suspend fun getUserFavorites(username: String, page: Int): List<Submission> {
        val nsfwEnabled = nsfwEnabledFlow.first()
        return filterSubmissions(FurAffinityScraper.getUserFavorites(username, page, nsfwEnabled))
    }

    suspend fun saveRecentSearches(searches: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[RECENT_SEARCHES_KEY] = searches.joinToString("|||")
        }
    }

    val recentSearchesFlow: kotlinx.coroutines.flow.Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[RECENT_SEARCHES_KEY] ?: ""
        if (raw.isNotEmpty()) raw.split("|||").filter { it.isNotEmpty() } else emptyList()
    }

    suspend fun addRecentSearch(query: String) {
        val current = loadRecentSearches().toMutableList()
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            current.remove(trimmedQuery)
            current.add(0, trimmedQuery)
            saveRecentSearches(current.take(20))
        }
    }

    // --- Appearance Preferences ---
    val defaultTabFlow: kotlinx.coroutines.flow.Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_TAB_KEY] ?: 0
    }

    suspend fun saveDefaultTab(tab: Int) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_TAB_KEY] = tab
        }
    }


    val themeFlow: kotlinx.coroutines.flow.Flow<com.furflix.app.ui.theme.ThemePreference> = context.dataStore.data.map { prefs ->
        com.furflix.app.ui.theme.ThemePreference.fromId(prefs[THEME_ID_KEY] ?: 0)
    }

    val isGridFlow: kotlinx.coroutines.flow.Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_GRID_KEY] ?: true
    }

    val appIconFlow: kotlinx.coroutines.flow.Flow<com.furflix.app.ui.theme.AppIcon> = context.dataStore.data.map { prefs ->
        com.furflix.app.ui.theme.AppIcon.fromId(prefs[APP_ICON_KEY] ?: "dark")
    }

    suspend fun saveAppIcon(icon: com.furflix.app.ui.theme.AppIcon) {
        context.dataStore.edit { prefs -> prefs[APP_ICON_KEY] = icon.id }
    }

    suspend fun saveTheme(themeId: Int) {
        context.dataStore.edit { prefs -> prefs[THEME_ID_KEY] = themeId }
    }

    suspend fun loadRecentSearches(): List<String> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[RECENT_SEARCHES_KEY] ?: ""
        return if (raw.isNotEmpty()) raw.split("|||").filter { it.isNotEmpty() } else emptyList()
    }

    suspend fun saveIsGrid(isGrid: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_GRID_KEY] = isGrid
        }
    }

    suspend fun loadIsGrid(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[IS_GRID_KEY] ?: true
    }

    // --- Content Filters ---

    val mutedWordsFlow: kotlinx.coroutines.flow.Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[MUTED_WORDS_KEY] ?: ""
        if (raw.isNotEmpty()) raw.split("|||") else emptyList()
    }

    suspend fun saveMutedWords(words: List<String>) {
        context.dataStore.edit { prefs -> prefs[MUTED_WORDS_KEY] = words.joinToString("|||") }
    }

    val nsfwEnabledFlow: kotlinx.coroutines.flow.Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NSFW_ENABLED_KEY] ?: true
    }

    suspend fun saveNsfwEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[NSFW_ENABLED_KEY] = enabled }
    }

    val downloadFolderFlow: kotlinx.coroutines.flow.Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOAD_FOLDER_KEY] ?: ""
    }

    suspend fun saveDownloadFolder(uriString: String) {
        context.dataStore.edit { prefs -> prefs[DOWNLOAD_FOLDER_KEY] = uriString }
    }
}