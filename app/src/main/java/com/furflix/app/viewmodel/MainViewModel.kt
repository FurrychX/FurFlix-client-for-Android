package com.furflix.app.viewmodel

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.furflix.app.data.model.Submission
import com.furflix.app.data.remote.FurAffinityScraper
import com.furflix.app.data.repository.FurRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FurRepository.getInstance(application)

    private val _browseSubmissions = MutableStateFlow<List<Submission>>(emptyList())
    val browseSubmissions: StateFlow<List<Submission>> = _browseSubmissions.asStateFlow()

    private val _latestSubmissions = MutableStateFlow<List<Submission>>(emptyList())
    val latestSubmissions: StateFlow<List<Submission>> = _latestSubmissions.asStateFlow()

    private val _searchSubmissions = MutableStateFlow<List<Submission>>(emptyList())
    val searchSubmissions: StateFlow<List<Submission>> = _searchSubmissions.asStateFlow()

    private val _submissionDetails = MutableStateFlow<Map<String, Submission>>(emptyMap())
    val submissionDetails: StateFlow<Map<String, Submission>> = _submissionDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _searchFilters = MutableStateFlow(com.furflix.app.data.model.SearchFilters())
    val searchFilters: StateFlow<com.furflix.app.data.model.SearchFilters> = _searchFilters.asStateFlow()

    private val _tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val tagSuggestions: StateFlow<List<String>> = _tagSuggestions.asStateFlow()

    private var searchTagsJob: kotlinx.coroutines.Job? = null

    fun fetchTagSuggestions(prefix: String) {
        searchTagsJob?.cancel()
        if (prefix.isBlank()) {
            _tagSuggestions.value = emptyList()
            return
        }
        searchTagsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300) // Debounce 300ms
            _tagSuggestions.value = repository.searchTags(prefix)
        }
    }

    fun clearTagSuggestions() {
        searchTagsJob?.cancel()
        _tagSuggestions.value = emptyList()
    }

    private val _nsfwEnabled = MutableStateFlow(true)
    val nsfwEnabled: StateFlow<Boolean> = _nsfwEnabled.asStateFlow()

    private val _downloadFolder = MutableStateFlow("")
    val downloadFolder: StateFlow<String> = _downloadFolder.asStateFlow()

    fun updateSearchFilters(filters: com.furflix.app.data.model.SearchFilters) {
        _searchFilters.value = filters
        val hasKeywords = filters.keywords.isNotEmpty()
        if (_searchQuery.value.isNotEmpty() || hasKeywords || _selectedTab.value == 2) {
            loadSearch(_searchQuery.value)
        } else if (_selectedTab.value == 0) {
            }
    }

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isTogglingFavorite = MutableStateFlow(false)
    val isTogglingFavorite: StateFlow<Boolean> = _isTogglingFavorite.asStateFlow()

    fun toggleFavorite(postId: String) {
        if (_isTogglingFavorite.value) return
        val details = _submissionDetails.value[postId] ?: return
        if (details.favUrl.isEmpty()) return
        val addToFavorites = !details.isFavorited
        val currentFavUrl = details.favUrl
        _isTogglingFavorite.value = true
        // Optimistic update — toggle state + swap the fav/unfav URL
        _submissionDetails.update { current ->
            val newMap = current.toMutableMap()
            newMap[postId] = details.copy(
                isFavorited = addToFavorites,
                favUrl = currentFavUrl.replace(
                    if (addToFavorites) "/unfav/" else "/fav/",
                    if (addToFavorites) "/fav/" else "/unfav/"
                ),
                favorites = details.favorites + (if (addToFavorites) 1 else -1)
            )
            newMap
        }
        viewModelScope.launch {
            try {
                val success = repository.toggleFavorite(currentFavUrl)
                if (!success) {
                    // Revert on failure
                    _submissionDetails.update { current ->
                        val newMap = current.toMutableMap()
                        newMap[postId] = details
                        newMap
                    }
                }
            } catch (_: Exception) {
                _submissionDetails.update { current ->
                    val newMap = current.toMutableMap()
                    newMap[postId] = details
                    newMap
                }
            }
            _isTogglingFavorite.value = false
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun resetSearch() {
        _hasSearched.value = false
        _searchQuery.value = ""
        _searchSubmissions.value = emptyList()
        _searchFilters.value = com.furflix.app.data.model.SearchFilters()
    }

    fun clearRecentSearches() {
        viewModelScope.launch { repository.saveRecentSearches(emptyList()) }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch { 
            val currentList = repository.loadRecentSearches().toMutableList()
            currentList.remove(query)
            repository.saveRecentSearches(currentList)
        }
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private var browsePage = 1
    private var latestPage = 1
    private var searchPage = 1
    private var watchlistPage = 1
    
    private var browseHasMore = true
    private var latestHasMore = true
    private var searchHasMore = true
    private var watchlistHasMore = true

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val TAB_BROWSE = 0
        const val TAB_LATEST = 1
        const val TAB_SEARCH = 2
        const val TAB_WATCHLIST = 3
    }

    init {
        com.furflix.app.data.local.TagsDatabase.getInstance(application).prefetch()
        viewModelScope.launch {
            val initialNsfw = repository.nsfwEnabledFlow.first()
            _nsfwEnabled.value = initialNsfw
            _searchFilters.value = _searchFilters.value.copy(
                ratingMature = initialNsfw,
                ratingAdult = initialNsfw
            )
            
            val defaultTab = repository.defaultTabFlow.first()
            _selectedTab.value = defaultTab
            // Trigger load based on default tab
            when (defaultTab) {
                0 -> loadBrowse()
                1 -> loadLatest()
            }
        }
        _isLoggedIn.value = FurAffinityScraper.isLoggedIn()
        viewModelScope.launch {
            repository.recentSearchesFlow.collect {
                _recentSearches.value = it
            }
        }
        viewModelScope.launch {
            repository.downloadFolderFlow.collect {
                _downloadFolder.value = it
            }
        }
        viewModelScope.launch {
            repository.nsfwEnabledFlow.collect { nsfw ->
                val changed = _nsfwEnabled.value != nsfw
                _nsfwEnabled.value = nsfw
                if (changed) {
                    _searchFilters.value = _searchFilters.value.copy(
                        ratingMature = nsfw,
                        ratingAdult = nsfw
                    )
                }
                if (changed && hasInitialized) {
                    when (_selectedTab.value) {
                        TAB_BROWSE -> loadBrowse()
                        TAB_LATEST -> loadLatest()
                        TAB_SEARCH -> loadSearch(_searchQuery.value)
                        TAB_WATCHLIST -> { /* Profile handles its own refresh */ }
                    }
                }
            }
        }
    }

    fun toggleNsfw() {
        viewModelScope.launch {
            repository.saveNsfwEnabled(!_nsfwEnabled.value)
        }
    }

    private var hasInitialized = false

    fun tryAutoLogin() {
        if (hasInitialized) return
        hasInitialized = true
        viewModelScope.launch {
            val success = repository.loadSavedCookies()
            if (success) {
                _isLoggedIn.value = true
                _username.value = repository.getUsername()
                if (_browseSubmissions.value.isEmpty()) {
                    loadBrowse()
                }
            } else if (_browseSubmissions.value.isEmpty()) {
                loadBrowse()
            }
        }
    }

    fun loginWithCookies(cookies: String, webViewUsername: String = "") {
        FurAffinityScraper.setCookiesFromWebView(cookies)
        viewModelScope.launch {
            var name = webViewUsername
            if (name.isEmpty() || name == "Logged in") {
                val fetchedName = FurAffinityScraper.verifyLoginAndGetUsername()
                if (!fetchedName.isNullOrEmpty()) {
                    name = fetchedName
                } else {
                    name = "Logged in"
                }
            }
            repository.saveCookiesFromWebView(cookies, name)
            _isLoggedIn.value = true
            _username.value = name
            
            when (_selectedTab.value) {
                TAB_BROWSE -> loadBrowse()
                TAB_LATEST -> loadLatest()
                TAB_SEARCH -> loadSearch(_searchQuery.value)
                TAB_WATCHLIST -> { /* Profile handles its own refresh */ }
                else -> loadBrowse()
            }
        }
    }

    fun loadBrowse() {
        browsePage = 1
        browseHasMore = true
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getBrowse(browsePage, _searchFilters.value)
                _browseSubmissions.value = result
                // Do not set errorMessage when empty — can happen briefly on startup
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun loadLatest() {
        latestPage = 1
        latestHasMore = true
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getLatest(latestPage)
                _latestSubmissions.value = result
                // Do not set errorMessage when empty
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun loadSearch(query: String) {
        val currentFilters = _searchFilters.value
        if (query.isBlank() && currentFilters.keywords.isEmpty()) return
        _searchQuery.value = query
        _hasSearched.value = true
        searchPage = 1
        searchHasMore = true

        val trimmed = query.trim()
        val itemsToAdd = mutableListOf<String>()
        if (trimmed.isNotEmpty()) itemsToAdd.add(trimmed)
        itemsToAdd.addAll(currentFilters.keywords)

        if (itemsToAdd.isNotEmpty()) {
            viewModelScope.launch {
                itemsToAdd.reversed().forEach { item ->
                    repository.addRecentSearch(item)
                }
            }
        }

        viewModelScope.launch {
            _searchSubmissions.value = emptyList()
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.search(query, searchPage, currentFilters)
                _searchSubmissions.value = result
                if (result.isEmpty()) {
                    _errorMessage.value = "No search results found."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun loadWatchlist() {
        watchlistPage = 1
        watchlistHasMore = true
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getWatchlist(1)
                _browseSubmissions.value = result // shared with Profile tab
                if (result.isEmpty()) {
                    _errorMessage.value = "Watchlist is empty or login required."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}. You may need to log in."
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val (page, result) = when (_selectedTab.value) {
                    TAB_BROWSE -> {
                        if (!browseHasMore) { _isLoadingMore.value = false; return@launch }
                        browsePage++
                        browsePage to repository.getBrowse(browsePage, _searchFilters.value)
                    }
                    TAB_LATEST -> {
                        if (!latestHasMore) { _isLoadingMore.value = false; return@launch }
                        latestPage++
                        latestPage to repository.getLatest(latestPage)
                    }
                    TAB_SEARCH -> {
                        if (!searchHasMore) { _isLoadingMore.value = false; return@launch }
                        searchPage++
                        searchPage to repository.search(_searchQuery.value, searchPage, _searchFilters.value)
                    }
                    else -> {
                        if (!browseHasMore) { _isLoadingMore.value = false; return@launch }
                        browsePage++
                        browsePage to repository.getBrowse(browsePage, _searchFilters.value)
                    }
                }
                if (result.isEmpty()) {
                    when (_selectedTab.value) {
                        TAB_BROWSE -> { browsePage--; browseHasMore = false }
                        TAB_LATEST -> { latestPage--; latestHasMore = false }
                        TAB_SEARCH -> { searchPage--; searchHasMore = false }
                    }
                } else {
                    when (_selectedTab.value) {
                        TAB_BROWSE -> {
                            val unique = result.filter { r -> _browseSubmissions.value.none { it.id == r.id } }
                            browseHasMore = unique.isNotEmpty()
                            if (unique.isEmpty()) browsePage-- else _browseSubmissions.value = _browseSubmissions.value + unique
                        }
                        TAB_LATEST -> {
                            val unique = result.filter { r -> _latestSubmissions.value.none { it.id == r.id } }
                            latestHasMore = unique.isNotEmpty()
                            if (unique.isEmpty()) latestPage-- else _latestSubmissions.value = _latestSubmissions.value + unique
                        }
                        TAB_SEARCH -> {
                            val unique = result.filter { r -> _searchSubmissions.value.none { it.id == r.id } }
                            searchHasMore = unique.isNotEmpty()
                            if (unique.isEmpty()) searchPage-- else _searchSubmissions.value = _searchSubmissions.value + unique
                        }
                    }
                }
            } catch (e: Exception) {
                when (_selectedTab.value) {
                    TAB_BROWSE -> browsePage--
                    TAB_LATEST -> latestPage--
                    TAB_SEARCH -> searchPage--
                }
            }
            _isLoadingMore.value = false
        }
    }

    fun loadSubmission(postId: String) {
        if (_submissionDetails.value.containsKey(postId)) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getSubmission(postId)
                if (result != null) {
                    _submissionDetails.update { currentMap ->
                        val newMap = currentMap.toMutableMap()
                        newMap[postId] = result
                        newMap
                    }
                } else {
                    _errorMessage.value = "Failed to load submission details."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadImage(url: String, fileName: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36")
                        .header("Referer", "https://www.furaffinity.net/")
                        .build()
                    val response = httpClient.newCall(request).execute()

                    if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

                    val body = response.body ?: throw Exception("Empty response")
                    val context = getApplication<Application>()
                    val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val mimeType = when {
                            safeFileName.endsWith(".gif", true) -> "image/gif"
                            safeFileName.endsWith(".png", true) -> "image/png"
                            safeFileName.endsWith(".webp", true) -> "image/webp"
                            else -> "image/jpeg"
                        }
                        val folderPref = _downloadFolder.value
                        val relativePath = if (folderPref.isNotEmpty()) {
                            val parts = folderPref.split("/")
                            if (parts.size > 1) {
                                parts[0] + "/" + parts.drop(1).joinToString("/")
                            } else {
                                Environment.DIRECTORY_PICTURES + "/" + folderPref
                            }
                        } else {
                            Environment.DIRECTORY_PICTURES + "/FurFlix"
                        }
                        
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        }
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            context.contentResolver.openOutputStream(it)?.use { os ->
                                body.byteStream().use { input ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        os.write(buffer, 0, bytesRead)
                                    }
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val folderPref = _downloadFolder.value
                        val baseDir = if (folderPref.startsWith("Downloads")) Environment.DIRECTORY_DOWNLOADS
                                      else if (folderPref.startsWith("DCIM")) Environment.DIRECTORY_DCIM
                                      else Environment.DIRECTORY_PICTURES
                        val subName = if (folderPref.contains("/")) folderPref.substringAfter("/") else "FurFlix"
                        
                        val dir = File(Environment.getExternalStoragePublicDirectory(baseDir), subName)
                        dir.mkdirs()
                        FileOutputStream(File(dir, safeFileName)).use { fos ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    fos.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Saved: $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
            _username.value = ""
            _browseSubmissions.value = emptyList()
            _latestSubmissions.value = emptyList()
            _searchSubmissions.value = emptyList()
        }
    }

    // --- Cache management ---
    private val _cacheSize = MutableStateFlow("Calculating...")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir.resolve("image_cache")
            val sizeBytes = cacheDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
            val formatted = when {
                sizeBytes >= 1_073_741_824L -> String.format(java.util.Locale.US, "%.1f GB", sizeBytes / 1_073_741_824.0)
                sizeBytes >= 1_048_576L     -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / 1_048_576.0)
                sizeBytes >= 1_024L         -> String.format(java.util.Locale.US, "%.1f KB", sizeBytes / 1_024.0)
                else                        -> "$sizeBytes B"
            }
            _cacheSize.value = formatted
        }
    }

    fun clearImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _isClearingCache.value = true
            try {
                // Clear memory + disk cache via factory reset
                com.furflix.app.ui.components.ImageLoaderFactory.reset()

                // Delete the cache directory entirely
                val cacheDir = getApplication<Application>().cacheDir.resolve("image_cache")
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()

                // Rebuild global Coil loader with fresh caches
                val ctx = getApplication<Application>()
                val newLoader = com.furflix.app.ui.components.ImageLoaderFactory.getInstance(ctx)
                    .newBuilder()
                    .build()
                coil.Coil.setImageLoader(newLoader)

                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Image cache cleared", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isClearingCache.value = false
                refreshCacheSize()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setDownloadFolder(folder: String) {
        viewModelScope.launch {
            repository.saveDownloadFolder(folder)
        }
    }
}
