package com.furflix.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.furflix.app.data.model.Submission
import com.furflix.app.data.model.WatchlistUser
import com.furflix.app.data.repository.FurRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FurRepository.getInstance(application)

    private val _followingList = MutableStateFlow<List<WatchlistUser>>(emptyList())
    val followingList: StateFlow<List<WatchlistUser>> = _followingList.asStateFlow()

    private val _followersList = MutableStateFlow<List<WatchlistUser>>(emptyList())
    val followersList: StateFlow<List<WatchlistUser>> = _followersList.asStateFlow()

    private val _favoritesList = MutableStateFlow<List<Submission>>(emptyList())
    val favoritesList: StateFlow<List<Submission>> = _favoritesList.asStateFlow()

    private val _followingHasMoreState = MutableStateFlow(false)
    val followingHasMore: StateFlow<Boolean> = _followingHasMoreState.asStateFlow()

    private val _followersHasMoreState = MutableStateFlow(false)
    val followersHasMore: StateFlow<Boolean> = _followersHasMoreState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loggedInUsername = MutableStateFlow("")
    val loggedInUsername: StateFlow<String> = _loggedInUsername.asStateFlow()

    companion object {
        const val TAB_FAVORITES = 0
        const val TAB_FOLLOWING = 1
        const val TAB_FOLLOWERS = 2
    }

    private val _currentTab = MutableStateFlow(TAB_FAVORITES)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private var followingPage = 1
    private var followersPage = 1
    private var favoritesPage = 1
    private var followingMore = true
    private var followersMore = true
    private var favoritesMore = true

    private var _lastNsfwEnabled: Boolean? = null

    init {
        _loggedInUsername.value = repository.getUsername()
        loadData()
        viewModelScope.launch {
            repository.nsfwEnabledFlow.collect { nsfw ->
                if (_lastNsfwEnabled != null && _lastNsfwEnabled != nsfw) {
                    _favoritesList.value = emptyList()
                    if (_currentTab.value == TAB_FAVORITES) {
                        loadData()
                    }
                }
                _lastNsfwEnabled = nsfw
            }
        }
    }

    fun setTab(tab: Int) {
        if (_currentTab.value != tab) {
            _currentTab.value = tab
            loadData()
        }
    }

    fun refreshIfNeeded() {
        val currentUsername = repository.getUsername()
        if (currentUsername != _loggedInUsername.value) {
            _loggedInUsername.value = currentUsername
            _favoritesList.value = emptyList()
            _followingList.value = emptyList()
            _followersList.value = emptyList()
            if (currentUsername.isNotEmpty()) {
                loadData()
            }
        } else if (currentUsername.isNotEmpty() && _errorMessage.value?.contains("log in", ignoreCase = true) == true) {
            _errorMessage.value = null
            loadData()
        }
    }

    private fun loadData() {
        val tab = _currentTab.value
        val currentList = when (tab) {
            TAB_FAVORITES -> _favoritesList.value
            TAB_FOLLOWING -> _followingList.value
            TAB_FOLLOWERS -> _followersList.value
            else -> emptyList()
        }
        if (currentList.isNotEmpty()) return

        when (tab) {
            TAB_FAVORITES -> { favoritesPage = 1; favoritesMore = true }
            TAB_FOLLOWING -> { followingPage = 1; followingMore = true }
            TAB_FOLLOWERS -> { followersPage = 1; followersMore = true }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val username = repository.getUsername()
                if (username.isEmpty()) {
                    _errorMessage.value = "Please log in to see your profile."
                    when (tab) {
                        TAB_FAVORITES -> favoritesMore = false
                        TAB_FOLLOWING -> followingMore = false
                        TAB_FOLLOWERS -> followersMore = false
                    }
                } else when (tab) {
                    TAB_FAVORITES -> {
                        val result = repository.getUserFavorites(username, favoritesPage)
                        _favoritesList.value = result
                        favoritesMore = result.isNotEmpty()
                        if (result.isEmpty()) _errorMessage.value = "No favorites yet."
                    }
                    TAB_FOLLOWING -> {
                        val (users, hasNext) = repository.getWatchlistBy(username, followingPage)
                        _followingList.value = users
                        followingMore = hasNext
                        _followingHasMoreState.value = hasNext
                        if (users.isEmpty()) _errorMessage.value = "You are not following anyone."
                    }
                    TAB_FOLLOWERS -> {
                        val (users, hasNext) = repository.getWatchlistTo(username, followersPage)
                        _followersList.value = users
                        followersMore = hasNext
                        _followersHasMoreState.value = hasNext
                        if (users.isEmpty()) _errorMessage.value = "You don't have any followers yet."
                    }
                }
            } catch (e: Exception) {
                Log.e("WatchlistVM", "loadData: exception", e)
                _errorMessage.value = "Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        val tab = _currentTab.value
        val hasMore = when (tab) {
            TAB_FAVORITES -> favoritesMore
            TAB_FOLLOWING -> followingMore
            TAB_FOLLOWERS -> followersMore
            else -> false
        }
        if (_isLoadingMore.value || !hasMore) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            when (tab) {
                TAB_FAVORITES -> favoritesPage++
                TAB_FOLLOWING -> followingPage++
                TAB_FOLLOWERS -> followersPage++
            }

            try {
                val username = repository.getUsername()
                when (tab) {
                    TAB_FAVORITES -> {
                        val more = repository.getUserFavorites(username, favoritesPage)
                        if (more.isEmpty()) {
                            // Truly no more results from FA
                            favoritesMore = false
                            favoritesPage--
                        } else {
                            val uniqueMore = more.filter { newSub -> _favoritesList.value.none { it.id == newSub.id } }
                            if (uniqueMore.isNotEmpty()) {
                                _favoritesList.value = _favoritesList.value + uniqueMore
                                favoritesMore = true
                            } else {
                                // All items were dupes (e.g. fallback fetched same page) — stop
                                favoritesMore = false
                                favoritesPage--
                            }
                        }
                    }
                    TAB_FOLLOWING -> {
                        val (more, hasNext) = repository.getWatchlistBy(username, followingPage)
                        followingMore = hasNext
                        _followingHasMoreState.value = hasNext
                        if (more.isNotEmpty()) {
                            _followingList.value = (_followingList.value + more)
                                .distinctBy { it.username.lowercase() }
                        }
                    }
                    TAB_FOLLOWERS -> {
                        val (more, hasNext) = repository.getWatchlistTo(username, followersPage)
                        followersMore = hasNext
                        _followersHasMoreState.value = hasNext
                        if (more.isNotEmpty()) {
                            _followersList.value = (_followersList.value + more)
                                .distinctBy { it.username.lowercase() }
                        }
                    }
                }
            } catch (e: Exception) {
                when (tab) {
                    TAB_FAVORITES -> favoritesPage--
                    TAB_FOLLOWING -> followingPage--
                    TAB_FOLLOWERS -> followersPage--
                }
            }
            _isLoadingMore.value = false
        }
    }
}
